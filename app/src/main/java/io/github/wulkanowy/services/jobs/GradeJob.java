package io.github.wulkanowy.services.jobs;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.io.IOException;

import io.github.wulkanowy.activity.WulkanowyApp;
import io.github.wulkanowy.api.Vulcan;
import io.github.wulkanowy.api.login.AccountPermissionException;
import io.github.wulkanowy.api.login.BadCredentialsException;
import io.github.wulkanowy.api.login.NotLoggedInErrorException;
import io.github.wulkanowy.dao.entities.DaoSession;
import io.github.wulkanowy.security.CryptoException;
import io.github.wulkanowy.services.LoginSession;
import io.github.wulkanowy.services.VulcanSynchronization;

public class GradeJob extends VulcanJobHelper {

    private static final String UNIQUE_TAG = "GradesSync34512";

    private static final int DEFAULT_INTERVAL_START = 60 * 50;

    private static final int DEFAULT_INTERVAL_END = DEFAULT_INTERVAL_START + (60 * 10);

    @Override
    protected Job createJob(FirebaseJobDispatcher dispatcher) {
        return dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setService(GradeService.class)
                .setTag(UNIQUE_TAG)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(DEFAULT_INTERVAL_START, DEFAULT_INTERVAL_END))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .setReplaceCurrent(true)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .build();
    }

    public static class GradeService extends VulcanService {

        @Override
        public void workToBePerformed() throws CryptoException, BadCredentialsException,
                NotLoggedInErrorException, AccountPermissionException, IOException {

            DaoSession daoSession = ((WulkanowyApp) getApplication()).getDaoSession();

            VulcanSynchronization vulcanSynchronization = new VulcanSynchronization(new LoginSession());
            vulcanSynchronization.loginCurrentUser(getApplicationContext(), daoSession, new Vulcan());
            vulcanSynchronization.syncGrades();
        }
    }
}
