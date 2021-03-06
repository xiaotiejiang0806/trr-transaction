package org.mengyun.compensable.transaction.retry.interceptor;

import org.mengyun.compensable.transaction.retry.RetryOperations;
import org.mengyun.compensable.transaction.retry.backoff.BackOffPolicy;
import org.mengyun.compensable.transaction.retry.backoff.ExponentialBackOffPolicy;
import org.mengyun.compensable.transaction.retry.policy.RetryPolicy;
import org.mengyun.compensable.transaction.retry.policy.SimpleRetryPolicy;
import org.mengyun.compensable.transaction.retry.support.RetryTemplate;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.util.Assert;

public abstract class RetryInterceptorBuilder<T extends MethodInterceptor> {

    protected final RetryTemplate retryTemplate = new RetryTemplate();

    protected final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();

    protected RetryOperations retryOperations;

    protected MethodInvocationRecoverer recoverer;

    protected MethodInvocationRecoverer rollbacker;

    private boolean templateAltered;

    private boolean backOffPolicySet;

    private boolean retryPolicySet;

    private boolean backOffOptionsSet;

    protected String label;

    /**
     * Create a builder for a stateless retry interceptor.
     *
     * @return The interceptor builder.
     */
    public static StatelessRetryInterceptorBuilder stateless() {
        return new StatelessRetryInterceptorBuilder();
    }

    /**
     * Apply the retry operations - once this is set, other properties can no longer be
     * set; can't be set if other properties have been applied.
     *
     * @param retryOperations The retry operations.
     * @return this.
     */
    public RetryInterceptorBuilder<T> retryOperations(RetryOperations retryOperations) {
        Assert.isTrue(!this.templateAltered,
                "Cannot set retryOperations when the default has been modified");
        this.retryOperations = retryOperations;
        return this;
    }

    /**
     * Apply the max attempts - a SimpleRetryPolicy will be used. Cannot be used if a custom retry operations
     * or retry policy has been set.
     *
     * @param maxAttempts the max attempts (including the initial attempt).
     * @return this.
     */
    public RetryInterceptorBuilder<T> maxAttempts(int maxAttempts) {
        Assert.isNull(this.retryOperations,
                "cannot alter the retry policy when a custom retryOperations has been set");
        Assert.isTrue(!this.retryPolicySet,
                "cannot alter the retry policy when a custom retryPolicy has been set");
        this.simpleRetryPolicy.setMaxAttempts(maxAttempts);
        this.retryTemplate.setRetryPolicy(this.simpleRetryPolicy);
        this.templateAltered = true;
        return this;
    }

    /**
     * Apply the backoff options. Cannot be used if a custom retry operations, or back off
     * policy has been set.
     *
     * @param initialInterval The initial interval.
     * @param multiplier      The multiplier.
     * @param maxInterval     The max interval.
     * @return this.
     */
    public RetryInterceptorBuilder<T> backOffOptions(long initialInterval,
                                                     double multiplier, long maxInterval) {
        Assert.isNull(this.retryOperations,
                "cannot set the back off policy when a custom retryOperations has been set");
        Assert.isTrue(!this.backOffPolicySet,
                "cannot set the back off options when a back off policy has been set");
        ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
        policy.setInitialInterval(initialInterval);
        policy.setMultiplier(multiplier);
        policy.setMaxInterval(maxInterval);
        this.retryTemplate.setBackOffPolicy(policy);
        this.backOffOptionsSet = true;
        this.templateAltered = true;
        return this;
    }

    /**
     * Apply the retry policy - cannot be used if a custom retry template has been
     * provided, or the max attempts or back off options or policy have been applied.
     *
     * @param policy The policy.
     * @return this.
     */
    public RetryInterceptorBuilder<T> retryPolicy(RetryPolicy policy) {
        Assert.isNull(this.retryOperations,
                "cannot set the retry policy when a custom retryOperations has been set");
        Assert.isTrue(!this.templateAltered,
                "cannot set the retry policy if max attempts or back off policy or options changed");
        this.retryTemplate.setRetryPolicy(policy);
        this.retryPolicySet = true;
        this.templateAltered = true;
        return this;
    }

    /**
     * Apply the back off policy. Cannot be used if a custom retry operations, or back off
     * policy has been applied.
     *
     * @param policy The policy.
     * @return this.
     */
    public RetryInterceptorBuilder<T> backOffPolicy(BackOffPolicy policy) {
        Assert.isNull(this.retryOperations,
                "cannot set the back off policy when a custom retryOperations has been set");
        Assert.isTrue(!this.backOffOptionsSet,
                "cannot set the back off policy when the back off policy options have been set");
        this.retryTemplate.setBackOffPolicy(policy);
        this.templateAltered = true;
        this.backOffPolicySet = true;
        return this;
    }


    public RetryInterceptorBuilder<T> recoverer(MethodInvocationRecoverer recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    public RetryInterceptorBuilder<T> rollback(MethodInvocationRecoverer rollbacker) {
        this.rollbacker = rollbacker;
        return this;
    }

    public RetryInterceptorBuilder<T> label(String label) {
        this.label = label;
        return this;
    }

    public abstract T build();

    private RetryInterceptorBuilder() {
    }


    public static class StatelessRetryInterceptorBuilder
            extends RetryInterceptorBuilder<RetryOperationsInterceptor> {

        private final RetryOperationsInterceptor interceptor = new RetryOperationsInterceptor();

        @Override
        public RetryOperationsInterceptor build() {
            if (this.recoverer != null) {
                this.interceptor.setRecoverer(this.recoverer);
            }
            if(this.rollbacker != null) {
                this.interceptor.setRollbacker(this.rollbacker);
            }
            if (this.retryOperations != null) {
                this.interceptor.setRetryOperations(this.retryOperations);
            } else {
                this.interceptor.setRetryOperations(this.retryTemplate);
            }
            if (this.label != null) {
                this.interceptor.setLabel(this.label);
            }
            return this.interceptor;
        }

        private StatelessRetryInterceptorBuilder() {
        }

    }

}
