import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.jbpm.services.task.HumanTaskConfigurator;
import org.jbpm.services.task.HumanTaskServiceFactory;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironment;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.task.TaskService;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

public class SampleProcessHaltingTest extends JbpmJUnitBaseTestCase {

	public SampleProcessHaltingTest() {
		super(true, true);
	}

	@Before
	public void initRuntime() throws IOException, NamingException, NotSupportedException, SystemException {

		UserGroupCallback userGroupCallback = new UserGroupCallback() {

			public List<String> getGroupsForUser(String userId, List<String> groupIds,
					List<String> allExistingGroupIds) {
				// TODO Auto-generated method stub
				return null;
			}

			public boolean existsUser(String userId) {
				// TODO Auto-generated method stub
				return true;
			}

			public boolean existsGroup(String groupId) {
				// TODO Auto-generated method stub
				return true;
			}
		};

		HumanTaskConfigurator configurator = HumanTaskServiceFactory.newTaskServiceConfigurator()
				.entityManagerFactory(getEmf()).userGroupCallback(userGroupCallback);

		TaskService internalTaskService = configurator.getTaskService();

		// Adding the envionrment entry because this is how the
		// RuntimeEnvironmentFactoryBean does it when using spring to add a task
		// service

		RuntimeEnvironment runtimeEnvironment = RuntimeEnvironmentBuilder.Factory.get()
				.newClasspathKmoduleDefaultBuilder().entityManagerFactory(getEmf()).userGroupCallback(userGroupCallback)
				.addEnvironmentEntry("org.kie.api.task.TaskService", internalTaskService).get();

		createRuntimeManager(Strategy.PROCESS_INSTANCE, null, runtimeEnvironment, null);
	}

	/**
	 * Test that shows bug - Launch process - create and then dispose of another
	 * runtime engine - complete task which should go to next task - show there
	 * are no more tasks even though there should be
	 */
	@Test
	public void sampleHaltingProcessTest() {
		RuntimeEngine engine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		engine.getKieSession().startProcess("samples.basic");
		TaskSummary ts = engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).iterator().next();

		RuntimeEngine toDispose = manager.getRuntimeEngine(ProcessInstanceIdContext.get());

		Assert.assertTrue(engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).size() == 1);

		manager.disposeRuntimeEngine(toDispose);

		Assert.assertTrue(engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).size() == 1);

		engine.getTaskService().start(ts.getId(), "user");

		engine.getTaskService().complete(ts.getId(), "user", null);
		Assert.assertTrue(engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).size() == 0);
	}

	/**
	 * Negative case. Same as previous example without creating and disposing of
	 * engine. Show that the second user task is created
	 */
	@Test
	public void sampleNegativeCaseNoHaltNoDispose() {
		RuntimeEngine engine = manager.getRuntimeEngine(ProcessInstanceIdContext.get());
		engine.getKieSession().startProcess("samples.basic");
		TaskSummary ts = engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).iterator().next();

		Assert.assertTrue(engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).size() == 1);

		engine.getTaskService().start(ts.getId(), "user");

		engine.getTaskService().complete(ts.getId(), "user", null);
		Assert.assertTrue(engine.getTaskService().getTasksAssignedAsPotentialOwner("user", null).size() == 1);
	}

}
