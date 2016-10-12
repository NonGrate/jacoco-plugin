package hudson.plugins.jacoco;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JacocoPublisherTest extends AbstractJacocoTestBase {
    private final TaskListener taskListener = niceMock(TaskListener.class);
    private final Launcher launcher = niceMock(Launcher.class);

    @Before
    public void setUp() {
        expect(taskListener.getLogger()).andReturn(System.out).anyTimes();
    }

	@Test
	public void testLocateReports() throws Exception {

		// Create a temporary workspace in the system 
		File w = File.createTempFile("workspace", ".test");
		assertTrue(w.delete());
		assertTrue(w.mkdir());
		w.deleteOnExit();
		FilePath workspace = new FilePath(w);

		// Create 4 files in the workspace
		File f1 = File.createTempFile("jacoco", ".xml", w);
		f1.deleteOnExit();
		File f2 = File.createTempFile("anyname", ".xml", w);
		f2.deleteOnExit();
		File f3 = File.createTempFile("jacoco", ".xml", w);
		f3.deleteOnExit();
		File f4 = File.createTempFile("anyname", ".xml", w);
		f4.deleteOnExit();


		// Create a folder and move there 2 files
		File d1 = new File(workspace.child("subdir").getRemote());
		assertTrue(d1.mkdir());
		d1.deleteOnExit();

		File f5 = new File(workspace.child(d1.getName()).child(f3.getName()).getRemote());
		File f6 = new File(workspace.child(d1.getName()).child(f4.getName()).getRemote());
		assertTrue(f3.renameTo(f5));
		assertTrue(f4.renameTo(f6));
		f5.deleteOnExit();
		f6.deleteOnExit();
		
		/*
		// Look for files in the entire workspace recursively without providing 
		// the includes parameter
		FilePath[] reports = JacocoPublisher.locateCoverageReports(workspace, "**e/jacoco*.xml");
		assertEquals(2 , reports.length);

		// Generate a includes string and look for files 
		String includes = f1.getName() + "; " + f2.getName() + "; " + d1.getName();
		reports = JacocoPublisher.locateCoverageReports(workspace, includes);
		assertEquals(3, reports.length);

		// Save files in local workspace
		FilePath local = workspace.child("coverage_localfolder");
		JacocoPublisher.saveCoverageReports(local, reports);
		assertEquals(3, local.list().size());
		
		local.deleteRecursive();
		 */

	}

	@Test
	public void testPerformWithDefaultSettings() throws IOException, InterruptedException {
		// expect
		final Run run = mock(Run.class);
		expect(run.getResult()).andReturn(Result.SUCCESS).anyTimes();
		expect(run.getEnvironment(taskListener)).andReturn(new EnvVars()).anyTimes();
		expect(run.getParent()).andReturn(null).anyTimes();
		Action action = anyObject();
		run.addAction(action);
		final AtomicReference<JacocoBuildAction> buildAction = new AtomicReference<JacocoBuildAction>();
		expectLastCall().andAnswer(new IAnswer<Void>() {
			@Override
			public Void answer() throws Throwable {
				buildAction.set((JacocoBuildAction) getCurrentArguments()[0]);
				buildAction.get().onAttached(run);

				return null;
			}
		});

        File dir = File.createTempFile("JaCoCoPublisherTest", ".tst");
        assertTrue(dir.delete());
        assertTrue(dir.mkdirs());
        assertTrue(new File(dir, "jacoco/classes").mkdirs());
        FilePath filePath = new FilePath(dir);

        expect(run.getRootDir()).andReturn(dir).anyTimes();

		replay(taskListener, run);

		// execute
		JacocoPublisher publisher = new JacocoPublisher();
		publisher.perform(run, filePath, launcher, taskListener);

		assertNotNull(buildAction.get());
		assertEquals(Result.SUCCESS, JacocoPublisher.checkResult(buildAction.get()));

        assertNotNull(run.toString());

        // verify
		verify(taskListener, run);
	}
}