/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.service;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.util.*;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.transaction.DebugXaResource;
import org.ofbiz.entity.transaction.GenericTransactionException;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.service.config.model.StartupService;
import org.ofbiz.service.eca.ServiceEcaRule;
import org.ofbiz.service.eca.ServiceEcaUtil;
import org.ofbiz.service.engine.GenericEngine;
import org.ofbiz.service.engine.GenericEngineFactory;
import org.ofbiz.service.job.JobManager;
import org.ofbiz.service.job.JobManagerException;
import org.ofbiz.service.semaphore.ServiceSemaphore;

import javax.transaction.Transaction;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The global service dispatcher. This is the "engine" part of the
 * Service Engine.
 */
public class ServiceDispatcher {

    public static final String module = ServiceDispatcher.class.getName();
    public static final int lruLogSize = 200;
    public static final int LOCK_RETRIES = 3;

    protected static final Map<RunningService, ServiceDispatcher> runLog = new ConcurrentLinkedHashMap.Builder<RunningService, ServiceDispatcher>().maximumWeightedCapacity(lruLogSize).build();
    protected static ConcurrentHashMap<String, ServiceDispatcher> dispatchers = new ConcurrentHashMap<String, ServiceDispatcher>();
    // FIXME: These fields are not thread-safe. They are modified by EntityDataLoadContainer.
    // We need a better design - like have this class query EntityDataLoadContainer if data is being loaded.
    protected static boolean enableJM = true;
    protected static boolean enableJMS = true;
    protected static boolean enableSvcs = true;

    protected Delegator delegator = null;
    protected GenericEngineFactory factory = null;
    protected Map<String, DispatchContext> localContext = new HashMap<String, DispatchContext>();
    protected Map<String, List<GenericServiceCallback>> callbacks = new HashMap<String, List<GenericServiceCallback>>();
    protected JobManager jm = null;

    protected ServiceDispatcher(Delegator delegator, boolean enableJM, boolean enableJMS) {
        factory = new GenericEngineFactory(this);;
        ServiceEcaUtil.readConfig();

        this.delegator = delegator;

        // clean up the service semaphores of same instance
        if (delegator != null) {
            try {
                int rn = delegator.removeByAnd("ServiceSemaphore", "lockedByInstanceId", JobManager.instanceId);
                if (rn > 0) {
                    Debug.logInfo("[ServiceDispatcher.init] : Clean up " + rn + " service semaphors.", module);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
        }

        // job manager needs to always be running, but the poller thread does not
        try {
            Delegator origDelegator = this.delegator;
            this.jm = JobManager.getInstance(origDelegator, enableJM);
        } catch (GeneralRuntimeException e) {
            Debug.logWarning(e.getMessage(), module);
        }
    }

    protected ServiceDispatcher(Delegator delegator) {
        this(delegator, enableJM, enableJMS);
    }

    /**
     * Returns a pre-registered instance of the ServiceDispatcher associated with this delegator.
     *
     * @param name      the name of the DispatchContext
     * @param delegator the local delegator
     * @return A reference to the LocalDispatcher associated with the DispatchContext
     */
    public static LocalDispatcher getLocalDispatcher(String name, Delegator delegator) {
        // get the ServiceDispatcher associated to the delegator (if not found in the cache, it will be created and added to the cache)
        ServiceDispatcher sd = getInstance(delegator);
        // if a DispatchContext has already been already registered as "name" then return the LocalDispatcher associated with it
        if (sd.containsContext(name)) {
            return sd.getLocalDispatcher(name);
        }
        // otherwise return null
        return null;
    }

    /**
     * Returns an instance of the ServiceDispatcher associated with this delegator.
     *
     * @param delegator the local delegator
     * @return A reference to this global ServiceDispatcher
     */
    public static ServiceDispatcher getInstance(Delegator delegator) {
        ServiceDispatcher sd;
        String dispatcherKey = delegator != null ? delegator.getDelegatorName() : "null";
        sd = dispatchers.get(dispatcherKey);
        if (sd == null) {
            if (Debug.verboseOn())
                Debug.logVerbose("[ServiceDispatcher.getInstance] : No instance found (" + dispatcherKey + ").", module);
            sd = new ServiceDispatcher(delegator);
            ServiceDispatcher cachedDispatcher = dispatchers.putIfAbsent(dispatcherKey, sd);
            if (cachedDispatcher == null) {
                // if the cachedDispatcher is null, then it means that
                // the new dispatcher created by this thread was successfully added to the cache
                // only in this case, the thread runs runStartupServices
                sd.runStartupServices();
                cachedDispatcher = sd;
            }
            sd = cachedDispatcher;
        }
        return sd;
    }

    /**
     * Registers the loader with this ServiceDispatcher
     *
     * @param context the context of the local dispatcher
     */
    public void register(DispatchContext context) {
        if (Debug.infoOn()) Debug.logInfo("Registering dispatcher: " + context.getName(), module);
        this.localContext.put(context.getName(), context);
    }

    /**
     * De-Registers the loader with this ServiceDispatcher
     *
     * @param local the LocalDispatcher to de-register
     */
    public void deregister(LocalDispatcher local) {
        if (Debug.infoOn()) Debug.logInfo("De-Registering dispatcher: " + local.getName(), module);
        localContext.remove(local.getName());
        if (localContext.size() == 0) {
            try {
                this.shutdown();
            } catch (GenericServiceException e) {
                Debug.logError(e, "Trouble shutting down ServiceDispatcher!", module);
            }
        }
    }

    public synchronized void registerCallback(String serviceName, GenericServiceCallback cb) {
        List<GenericServiceCallback> callBackList = callbacks.get(serviceName);
        if (callBackList == null) {
            callBackList = new LinkedList<GenericServiceCallback>();
        }
        callBackList.add(cb);
        callbacks.put(serviceName, callBackList);
    }

    public List<GenericServiceCallback> getCallbacks(String serviceName) {
        return callbacks.get(serviceName);
    }

    /**
     * Run the service synchronously and return the result.
     *
     * @param localName Name of the context to use.
     * @param service   Service model object.
     * @param context   Map of name, value pairs composing the context.
     * @return Map of name, value pairs composing the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public Map<String, Object> runSync(String localName, ModelService service, Map<String, ? extends Object> context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        return runSync(localName, service, context, true);
    }

    /**
     * Run the service synchronously and IGNORE the result.
     *
     * @param localName Name of the context to use.
     * @param service   Service model object.
     * @param context   Map of name, value pairs composing the context.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runSyncIgnore(String localName, ModelService service, Map<String, ? extends Object> context) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        runSync(localName, service, context, false);
    }

    /**
     * Run the service synchronously and return the result.
     *
     * @param localName    Name of the context to use.
     * @param modelService Service model object.
     * @param params       Map of name, value pairs composing the parameters.
     * @param validateOut  Validate OUT parameters
     * @return Map of name, value pairs composing the result.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public Map<String, Object> runSync(String localName, ModelService modelService, Map<String, ? extends Object> params, boolean validateOut) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        long serviceStartTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<String, Object>();
        ServiceSemaphore lock = null;
        Map<String, List<ServiceEcaRule>> eventMap = null;
        Map<String, Object> ecaContext = null;
        RunningService rs = null;
        DispatchContext ctx = localContext.get(localName);

        GenericEngine engine = null;
        Transaction parentTransaction = null;
        boolean isFailure = false;
        boolean isError = false;
        boolean beganTrans = false;
        try {
            // check for semaphore and acquire a lock
            if ("wait".equals(modelService.semaphore) || "fail".equals(modelService.semaphore)) {
                lock = new ServiceSemaphore(delegator, modelService);
                lock.acquire();
            }

            if (Debug.verboseOn() || modelService.debug) {
                Debug.logVerbose("[ServiceDispatcher.runSync] : invoking service " + modelService.name + " [" + modelService.location +
                        "/" + modelService.invoke + "] (" + modelService.engineName + ")", module);
            }

            Map<String, Object> context = new HashMap<String, Object>();
            if (params != null) {
                context.putAll(params);
            }
            // check the locale
            Locale locale = this.checkLocale(context);

            // set up the running service log
            rs = this.logService(localName, modelService, GenericEngine.SYNC_MODE);

            // get eventMap once for all calls for speed, don't do event calls if it is null
            eventMap = ServiceEcaUtil.getServiceEventMap(modelService.name);
            engine = this.getGenericEngine(modelService.engineName);


            // set IN attributes with default-value as applicable
            modelService.updateDefaultValues(context, ModelService.IN_PARAM);
            //Debug.logInfo("=========================== " + modelService.name + " 1 tx status =" + TransactionUtil.getStatusString() + ", modelService.requireNewTransaction=" + modelService.requireNewTransaction + ", modelService.useTransaction=" + modelService.useTransaction + ", TransactionUtil.isTransactionInPlace()=" + TransactionUtil.isTransactionInPlace(), module);
            if (modelService.useTransaction) {
                if (TransactionUtil.isTransactionInPlace()) {
                    // if a new transaction is needed, do it here; if not do nothing, just use current tx
                    if (modelService.requireNewTransaction) {
                        parentTransaction = TransactionUtil.suspend();
                        if (TransactionUtil.isTransactionInPlace()) {
                            throw new GenericTransactionException("In service " + modelService.name + " transaction is still in place after suspend, status is " + TransactionUtil.getStatusString());
                        }
                        // now start a new transaction
                        beganTrans = TransactionUtil.begin(modelService.transactionTimeout);
                    }
                } else {
                    beganTrans = TransactionUtil.begin(modelService.transactionTimeout);
                }
                // enlist for XAResource debugging
                if (beganTrans && TransactionUtil.debugResources()) {
                    DebugXaResource dxa = new DebugXaResource(modelService.name);
                    try {
                        dxa.enlist();
                    } catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
            }

            try {
                int lockRetriesRemaining = LOCK_RETRIES;
                boolean needsLockRetry = false;

                do {
                    // Ensure this is reset to false on each pass
                    needsLockRetry = false;

                    lockRetriesRemaining--;

                    // NOTE: general pattern here is to do everything up to the main service call, and retry it all if
                    //needed because those will be part of the same transaction and have been rolled back
                    // TODO: if there is an ECA called async or in a new transaction it won't get rolled back
                    //but will be called again, which means the service may complete multiple times! that would be for
                    //pre-invoke and earlier events only of course


                    // setup global transaction ECA listeners to execute later
                    if (eventMap != null)
                        ServiceEcaUtil.evalRules(modelService.name, eventMap, "global-rollback", ctx, context, result, isError, isFailure);
                    if (eventMap != null)
                        ServiceEcaUtil.evalRules(modelService.name, eventMap, "global-commit", ctx, context, result, isError, isFailure);

                    // pre-auth ECA
                    if (eventMap != null)
                        ServiceEcaUtil.evalRules(modelService.name, eventMap, "auth", ctx, context, result, isError, isFailure);

                    // check for pre-auth failure/errors
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] pre-auth ECA, before auth; isFailure=" + isFailure + ", isError=" + isError, module);
                    Object ticket = params.get("ticket");

                    if (modelService.auth && ticket == null) {
                        throw new ServiceAuthException("User authorization is required for this service: " + modelService.name + modelService.debugInfo());
                    }

                    // pre-validate ECA
                    if (eventMap != null)
                        ServiceEcaUtil.evalRules(modelService.name, eventMap, "in-validate", ctx, context, result, isError, isFailure);

                    // check for pre-validate failure/errors
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] pre-in-validate ECA, before in-validate; isFailure=" + isFailure + ", isError=" + isError, module);

                    // validate the context
                    if (modelService.validate && !isError && !isFailure) {
                        try {
                            modelService.validate(context, ModelService.IN_PARAM, locale);
                        } catch (ServiceValidationException e) {
                            Debug.logError(e, "Incoming context (in runSync : " + modelService.name + ") does not match expected requirements", module);
                            throw e;
                        }
                    }

                    // pre-invoke ECA
                    if (eventMap != null)
                        ServiceEcaUtil.evalRules(modelService.name, eventMap, "invoke", ctx, context, result, isError, isFailure);

                    // check for pre-invoke failure/errors
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] pre-invoke ECA, before invoke; isFailure=" + isFailure + ", isError=" + isError, module);

                    // ===== invoke the service =====
                    if (!isError && !isFailure) {
                        Map<String, Object> invokeResult = null;
                        invokeResult = engine.runSync(localName, modelService, context);
                        engine.sendCallbacks(modelService, context, invokeResult, GenericEngine.SYNC_MODE);
                        if (invokeResult != null) {
                            result.putAll(invokeResult);
                        } else {
                            Debug.logWarning("Service (in runSync : " + modelService.name + ") returns null result", module);
                        }
                    }

                    // re-check the errors/failures
                    isFailure = ServiceUtil.isFailure(result);
                    isError = ServiceUtil.isError(result);

                    //Debug.logInfo("After [" + modelService.name + "] invoke; isFailure=" + isFailure + ", isError=" + isError, module);

                    if (beganTrans) {
                        // crazy stuff here: see if there was a deadlock or other such error and if so retry... which we can ONLY do if we own the transaction!

                        String errMsg = ServiceUtil.getErrorMessage(result);

                        // look for the string DEADLOCK in an upper-cased error message; tested on: Derby, MySQL
                        // - Derby 10.2.2.0 deadlock string: "A lock could not be obtained due to a deadlock"
                        // - MySQL ? deadlock string: "Deadlock found when trying to get lock; try restarting transaction"
                        // - Postgres ? deadlock string: TODO
                        // - Other ? deadlock string: TODO
                        // TODO need testing in other databases because they all return different error messages for this!

                        // NOTE DEJ20070908 are there other things we need to check? I don't think so because these will
                        //be Entity Engine errors that will be caught and come back in an error message... IFF the
                        //service is written to not ignore it of course!
                        if (errMsg != null && errMsg.toUpperCase().indexOf("DEADLOCK") >= 0) {
                            // it's a deadlock! retry...
                            String retryMsg = "RETRYING SERVICE [" + modelService.name + "]: Deadlock error found in message [" + errMsg + "]; retry [" + (LOCK_RETRIES - lockRetriesRemaining) + "] of [" + LOCK_RETRIES + "]";

                            // make sure the old transaction is rolled back, and then start a new one

                            // if there is an exception in these things, let the big overall thing handle it
                            TransactionUtil.rollback(beganTrans, retryMsg, null);

                            beganTrans = TransactionUtil.begin(modelService.transactionTimeout);
                            // enlist for XAResource debugging
                            if (beganTrans && TransactionUtil.debugResources()) {
                                DebugXaResource dxa = new DebugXaResource(modelService.name);
                                try {
                                    dxa.enlist();
                                } catch (Exception e) {
                                    Debug.logError(e, module);
                                }
                            }

                            if (!beganTrans) {
                                // just log and let things roll through, will be considered an error and ECAs, etc will run according to that
                                Debug.logError("After rollback attempt for lock retry did not begin a new transaction!", module);
                            } else {
                                // deadlocks can be resolved by retring immediately as conflicting operations in the other thread will have cleared
                                needsLockRetry = true;

                                // reset state variables
                                result = new HashMap<String, Object>();
                                isFailure = false;
                                isError = false;

                                Debug.logWarning(retryMsg, module);
                            }

                            // look for lock wait timeout error, retry in a different way by running after the parent transaction finishes, ie attach to parent tx
                            // - Derby 10.2.2.0 lock wait timeout string: "A lock could not be obtained within the time requested"
                            // - MySQL ? lock wait timeout string: "Lock wait timeout exceeded; try restarting transaction"
                            if (errMsg != null && (errMsg.indexOf("A lock could not be obtained within the time requested") >= 0 ||
                                    errMsg.indexOf("Lock wait timeout exceeded") >= 0)) {
                                // TODO: add to run after parent tx
                            }
                        }
                    }
                } while (needsLockRetry && lockRetriesRemaining > 0);

                // create a new context with the results to pass to ECA services; necessary because caller may reuse this context
                ecaContext = new HashMap<String, Object>();
                ecaContext.putAll(context);
                // copy all results: don't worry parameters that aren't allowed won't be passed to the ECA services
                ecaContext.putAll(result);

                // setup default OUT values
                modelService.updateDefaultValues(context, ModelService.OUT_PARAM);

                // validate the result
                if (modelService.validate && validateOut) {
                    // pre-out-validate ECA
                    if (eventMap != null)
                        ServiceEcaUtil.evalRules(modelService.name, eventMap, "out-validate", ctx, ecaContext, result, isError, isFailure);
                    try {
                        modelService.validate(result, ModelService.OUT_PARAM, locale);
                    } catch (ServiceValidationException e) {
                        throw new GenericServiceException("Outgoing result (in runSync : " + modelService.name + ") does not match expected requirements", e);
                    }
                }

                // pre-commit ECA
                if (eventMap != null)
                    ServiceEcaUtil.evalRules(modelService.name, eventMap, "commit", ctx, ecaContext, result, isError, isFailure);

                // check for pre-commit failure/errors
                isFailure = ServiceUtil.isFailure(result);
                isError = ServiceUtil.isError(result);

                // global-commit-post-run ECA, like global-commit but gets the context after the service is run
                if (eventMap != null)
                    ServiceEcaUtil.evalRules(modelService.name, eventMap, "global-commit-post-run", ctx, ecaContext, result, isError, isFailure);

                // check for failure and log on info level; this is used for debugging
                if (isFailure) {
                    Debug.logWarning("Service Failure [" + modelService.name + "]: " + ServiceUtil.getErrorMessage(result), module);
                }
            } catch (Throwable t) {
                if (Debug.timingOn()) {
                    UtilTimer.closeTimer(localName + " / " + modelService.name, "Sync service failed...", module);
                }
                String errMsg = "Service [" + modelService.name + "] threw an unexpected exception/error";
                engine.sendCallbacks(modelService, context, t, GenericEngine.SYNC_MODE);
                try {
                    TransactionUtil.rollback(beganTrans, errMsg, t);
                } catch (GenericTransactionException te) {
                    Debug.logError(te, "Cannot rollback transaction", module);
                }
                rs.setEndStamp();
                if (t instanceof ServiceAuthException) {
                    throw (ServiceAuthException) t;
                } else if (t instanceof ServiceValidationException) {
                    throw (ServiceValidationException) t;
                } else if (t instanceof GenericServiceException) {
                    throw (GenericServiceException) t;
                } else {
                    throw new GenericServiceException("Service [" + modelService.name + "] Failed" + modelService.debugInfo(), t);
                }
            } finally {
                // if there was an error, rollback transaction, otherwise commit
                if (isError) {
                    String errMsg = "Error in Service [" + modelService.name + "]: " + ServiceUtil.getErrorMessage(result);
                    Debug.logError(errMsg, module);

                    // rollback the transaction
                    try {
                        TransactionUtil.rollback(beganTrans, errMsg, null);
                    } catch (GenericTransactionException e) {
                        Debug.logError(e, "Could not rollback transaction: " + e.toString(), module);
                    }

                    // 抛出去，否则接不到
                    throw new GenericServiceException(ServiceUtil.getErrorMessage(result));
                } else {
                    // commit the transaction
                    try {
                        TransactionUtil.commit(beganTrans);
                    } catch (GenericTransactionException e) {
                        GenericDelegator.popUserIdentifier();
                        String errMsg = "Could not commit transaction for service [" + modelService.name + "] call";
                        Debug.logError(e, errMsg, module);
                        if (e.getMessage() != null) {
                            errMsg = errMsg + ": " + e.getMessage();
                        }
                        throw new GenericServiceException(errMsg);
                    }
                }

                // call notifications -- event is determined from the result (success, error, fail)
                modelService.evalNotifications(this.getLocalContext(localName), context, result);

                // clear out the EE userIdentifier
                GenericDelegator.popUserIdentifier();
            }
        } catch (GenericTransactionException te) {
            Debug.logError(te, "Problems with the transaction", module);
            throw new GenericServiceException("Problems with the transaction.", te.getNested());
        } finally {
            if (lock != null) {
                // release the semaphore lock
                try {
                    lock.release();
                } catch (GenericServiceException e) {
                    Debug.logWarning(e, "Exception thrown while unlocking semaphore: ", module);
                }
            }

            // resume the parent transaction
            if (parentTransaction != null) {
                try {
                    TransactionUtil.resume(parentTransaction);
                } catch (GenericTransactionException ite) {
                    Debug.logWarning(ite, "Transaction error, not resumed", module);
                    throw new GenericServiceException("Resume transaction exception, see logs");
                }
            }
        }

        // pre-return ECA
        if (eventMap != null)
            ServiceEcaUtil.evalRules(modelService.name, eventMap, "return", ctx, ecaContext, result, isError, isFailure);

        rs.setEndStamp();

        long timeToRun = System.currentTimeMillis() - serviceStartTime;
        long showServiceDurationThreshold = UtilProperties.getPropertyAsLong("service", "showServiceDurationThreshold", 0);
        long showSlowServiceThreshold = UtilProperties.getPropertyAsLong("service", "showSlowServiceThreshold", 1000);

        if (Debug.timingOn() && timeToRun > showServiceDurationThreshold) {
            Debug.logTiming("Sync service [" + localName + "/" + modelService.name + "] finished in [" + timeToRun + "] milliseconds", module);
        } else if (Debug.infoOn() && timeToRun > showSlowServiceThreshold) {
            Debug.logTiming("Slow sync service execution detected: service [" + localName + "/" + modelService.name + "] finished in [" + timeToRun + "] milliseconds", module);
        }
        if ((Debug.verboseOn() || modelService.debug) && timeToRun > 50 && !modelService.hideResultInLog) {
            // Sanity check - some service results can be multiple MB in size. Limit message size to 10K.
            String resultStr = result.toString();
            if (resultStr.length() > 10240) {
                resultStr = resultStr.substring(0, 10226) + "...[truncated]";
            }
            Debug.logVerbose("Sync service [" + localName + "/" + modelService.name + "] finished with response [" + resultStr + "]", module);
        }
        if (modelService.metrics != null) {
            modelService.metrics.recordServiceRate(1, timeToRun);
        }

        if (modelService.formatInfo.keySet() == null || modelService.formatInfo.keySet().size() != 0) {
            // result.put("format", format);
        }

        return result;
    }

    /**
     * Run the service asynchronously, passing an instance of GenericRequester that will receive the result.
     *
     * @param localName Name of the context to use.
     * @param service   Service model object.
     * @param params    Map of name, value pairs composing the parameters.
     * @param requester Object implementing GenericRequester interface which will receive the result.
     * @param persist   True for store/run; False for run.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService service, Map<String, ? extends Object> params, GenericRequester requester, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        if (Debug.timingOn()) {
            UtilTimer.timerLog(localName + " / " + service.name, "ASync service started...", module);
        }
        if (Debug.verboseOn() || service.debug) {
            Debug.logVerbose("[ServiceDispatcher.runAsync] : preparing service " + service.name + " [" + service.location + "/" + service.invoke +
                    "] (" + service.engineName + ")", module);
        }

        Map<String, Object> context = new HashMap<String, Object>();
        if (params != null) {
            context.putAll(params);
        }
        // setup the result map
        Map<String, Object> result = new HashMap<String, Object>();
        boolean isFailure = false;
        boolean isError = false;

        // set up the running service log
        this.logService(localName, service, GenericEngine.ASYNC_MODE);

        // check the locale
        Locale locale = this.checkLocale(context);

        // setup the engine and context
        DispatchContext ctx = localContext.get(localName);
        GenericEngine engine = this.getGenericEngine(service.engineName);

        // for isolated transactions
        Transaction parentTransaction = null;
        // start the transaction
        boolean beganTrans = false;

        try {
            if (service.useTransaction) {
                if (TransactionUtil.isTransactionInPlace()) {
                    // if a new transaction is needed, do it here; if not do nothing, just use current tx
                    if (service.requireNewTransaction) {
                        parentTransaction = TransactionUtil.suspend();
                        // now start a new transaction
                        beganTrans = TransactionUtil.begin(service.transactionTimeout);
                    }
                } else {
                    beganTrans = TransactionUtil.begin(service.transactionTimeout);
                }
                // enlist for XAResource debugging
                if (beganTrans && TransactionUtil.debugResources()) {
                    DebugXaResource dxa = new DebugXaResource(service.name);
                    try {
                        dxa.enlist();
                    } catch (Exception e) {
                        Debug.logError(e, module);
                    }
                }
            }

            try {
                // get eventMap once for all calls for speed, don't do event calls if it is null
                Map<String, List<ServiceEcaRule>> eventMap = ServiceEcaUtil.getServiceEventMap(service.name);

                // pre-auth ECA
                if (eventMap != null)
                    ServiceEcaUtil.evalRules(service.name, eventMap, "auth", ctx, context, result, isError, isFailure);

                Object ticket = params.get("ticket");

                if (service.auth && ticket == null) {
                    throw new ServiceAuthException("User authorization is required for this service: " + service.name + service.debugInfo());
                }

                // pre-validate ECA
                if (eventMap != null)
                    ServiceEcaUtil.evalRules(service.name, eventMap, "in-validate", ctx, context, result, isError, isFailure);

                // check for pre-validate failure/errors
                isFailure = ModelService.RESPOND_FAIL.equals(result.get(ModelService.RESPONSE_MESSAGE));
                isError = ModelService.RESPOND_ERROR.equals(result.get(ModelService.RESPONSE_MESSAGE));

                // validate the context
                if (service.validate && !isError && !isFailure) {
                    try {
                        service.validate(context, ModelService.IN_PARAM, locale);
                    } catch (ServiceValidationException e) {
                        Debug.logError(e, "Incoming service context (in runAsync: " + service.name + ") does not match expected requirements", module);
                        throw e;
                    }
                }

                // run the service
                if (!isError && !isFailure) {
                    if (requester != null) {
                        engine.runAsync(localName, service, context, requester, persist);
                    } else {
                        engine.runAsync(localName, service, context, persist);
                    }
                    engine.sendCallbacks(service, context, GenericEngine.ASYNC_MODE);
                }

                if (Debug.timingOn()) {
                    UtilTimer.closeTimer(localName + " / " + service.name, "ASync service finished...", module);
                }
            } catch (Throwable t) {
                if (Debug.timingOn()) {
                    UtilTimer.closeTimer(localName + " / " + service.name, "ASync service failed...", module);
                }
                String errMsg = "Service [" + service.name + "] threw an unexpected exception/error";
                Debug.logError(t, errMsg, module);
                engine.sendCallbacks(service, context, t, GenericEngine.ASYNC_MODE);
                try {
                    TransactionUtil.rollback(beganTrans, errMsg, t);
                } catch (GenericTransactionException te) {
                    Debug.logError(te, "Cannot rollback transaction", module);
                }
                if (t instanceof ServiceAuthException) {
                    throw (ServiceAuthException) t;
                } else if (t instanceof ServiceValidationException) {
                    throw (ServiceValidationException) t;
                } else if (t instanceof GenericServiceException) {
                    throw (GenericServiceException) t;
                } else {
                    throw new GenericServiceException("Service [" + service.name + "] Failed" + service.debugInfo(), t);
                }
            } finally {
                // always try to commit the transaction since we don't know in this case if its was an error or not
                try {
                    TransactionUtil.commit(beganTrans);
                } catch (GenericTransactionException e) {
                    Debug.logError(e, "Could not commit transaction", module);
                    throw new GenericServiceException("Commit transaction failed");
                }
            }
        } catch (GenericTransactionException se) {
            Debug.logError(se, "Problems with the transaction", module);
            throw new GenericServiceException("Problems with the transaction: " + se.getMessage() + "; See logs for more detail");
        } finally {
            // resume the parent transaction
            if (parentTransaction != null) {
                try {
                    TransactionUtil.resume(parentTransaction);
                } catch (GenericTransactionException ise) {
                    Debug.logError(ise, "Trouble resuming parent transaction", module);
                    throw new GenericServiceException("Resume transaction exception: " + ise.getMessage() + "; See logs for more detail");
                }
            }
        }
    }

    /**
     * Run the service asynchronously and IGNORE the result.
     *
     * @param localName Name of the context to use.
     * @param service   Service model object.
     * @param context   Map of name, value pairs composing the context.
     * @param persist   True for store/run; False for run.
     * @throws ServiceAuthException
     * @throws ServiceValidationException
     * @throws GenericServiceException
     */
    public void runAsync(String localName, ModelService service, Map<String, ? extends Object> context, boolean persist) throws ServiceAuthException, ServiceValidationException, GenericServiceException {
        this.runAsync(localName, service, context, null, persist);
    }

    /**
     * Gets the GenericEngine instance that corresponds to the given name
     *
     * @param engineName Name of the engine
     * @return GenericEngine instance that corresponds to the engineName
     */
    public GenericEngine getGenericEngine(String engineName) throws GenericServiceException {
        return factory.getGenericEngine(engineName);
    }

    /**
     * Gets the JobManager associated with this dispatcher
     *
     * @return JobManager that is associated with this dispatcher
     */
    public JobManager getJobManager() {
        return this.jm;
    }

    /**
     * Gets the Delegator associated with this dispatcher
     *
     * @return Delegator associated with this dispatcher
     */
    public Delegator getDelegator() {
        return this.delegator;
    }

    /**
     * Gets the local context from a name
     *
     * @param name of the context to find.
     */
    public DispatchContext getLocalContext(String name) {
        return localContext.get(name);
    }

    /**
     * Gets the local dispatcher from a name
     *
     * @param name of the LocalDispatcher to find.
     * @return LocalDispatcher matching the loader name
     */
    public LocalDispatcher getLocalDispatcher(String name) {
        return localContext.get(name).getDispatcher();
    }

    /**
     * Test if this dispatcher instance contains the local context.
     *
     * @param name of the local context
     * @return true if the local context is found in this dispatcher.
     */
    public boolean containsContext(String name) {
        return localContext.containsKey(name);
    }

    protected void shutdown() throws GenericServiceException {
        Debug.logImportant("Shutting down the service engine...", module);
    }

    // checks the locale object in the context
    private Locale checkLocale(Map<String, Object> context) {
        Object locale = context.get("locale");
        Locale newLocale = null;

        if (locale != null) {
            if (locale instanceof Locale) {
                return (Locale) locale;
            } else if (locale instanceof String) {
                // en_US = lang_COUNTRY
                newLocale = UtilMisc.parseLocale((String) locale);
            }
        }

        if (newLocale == null) {
            newLocale = Locale.getDefault();
        }
        context.put("locale", newLocale);
        return newLocale;
    }

    // run startup services
    private synchronized int runStartupServices() {
        if (!enableSvcs || jm == null) {
            return 0;
        }
        int servicesScheduled = 0;
        List<StartupService> startupServices = null;
        try {
            startupServices = ServiceConfigUtil.getServiceEngine().getStartupServices();
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting service config: ", module);
            return 0;
        }
        for (StartupService startupService : startupServices) {
            String serviceName = startupService.getName();
            String runtimeDataId = startupService.getRuntimeDataId();
            int runtimeDelay = startupService.getRuntimeDelay();
            String sendToPool = startupService.getRunInPool();
            if (UtilValidate.isEmpty(sendToPool)) {
                try {
                    sendToPool = ServiceConfigUtil.getServiceEngine().getThreadPool().getSendToPool();
                } catch (GenericConfigException e) {
                    Debug.logError(e, "Unable to get send pool in service [" + serviceName + "]: ", module);
                }
            }
            // current time + 1 sec delay + extended delay
            long runtime = System.currentTimeMillis() + 1000 + runtimeDelay;
            try {
                jm.schedule(sendToPool, serviceName, runtimeDataId, runtime);
            } catch (JobManagerException e) {
                Debug.logError(e, "Unable to schedule service [" + serviceName + "]", module);
            }
        }

        return servicesScheduled;
    }

    private RunningService logService(String localName, ModelService modelService, int mode) {
        // set up the running service log
        RunningService rs = new RunningService(localName, modelService, mode);
        runLog.put(rs, this);
        return rs;
    }

    /**
     * Enables/Disables the Job Manager/Scheduler globally
     * (this will not effect any dispatchers already running)
     *
     * @param enable
     */
    public static void enableJM(boolean enable) {
        ServiceDispatcher.enableJM = enable;
    }

    /**
     * Enables/Disables the JMS listeners globally
     * (this will not effect any dispatchers already running)
     *
     * @param enable
     */
    public static void enableJMS(boolean enable) {
        ServiceDispatcher.enableJMS = enable;
    }

    /**
     * Enables/Disables the startup services globally
     * (this will not effect any dispatchers already running)
     *
     * @param enable
     */
    public static void enableSvcs(boolean enable) {
        ServiceDispatcher.enableSvcs = enable;
    }

    public static Map<RunningService, ServiceDispatcher> getServiceLogMap() {
        return runLog;
    }

}
