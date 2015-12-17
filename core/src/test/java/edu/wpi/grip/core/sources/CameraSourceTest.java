package edu.wpi.grip.core.sources;


import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import edu.wpi.grip.core.GRIPCoreModule;
import edu.wpi.grip.core.events.UnexpectedThrowableEvent;
import org.bytedeco.javacpp.indexer.Indexer;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CameraSourceTest {
    private Injector injector;
    private CameraSource.Factory cameraSourceFactory;

    private EventBus eventBus;
    private CameraSource cameraSourceWithMockGrabber;
    private MockFrameGrabberFactory mockFrameGrabberFactory;


    class MockFrameGrabber extends FrameGrabber {
        private final Frame frame;
        private final Indexer frameIdx;
        private boolean shouldThrowAtStart = false;

        MockFrameGrabber() {
            this.frame = new Frame(640 + 1, 480, Frame.DEPTH_SHORT, 3);
            this.frameIdx = frame.createIndexer();
            for (int y = 0; y < frameIdx.rows(); y++) {
                for (int x = 0; x < frameIdx.cols(); x++) {
                    for (int z = 0; z < frameIdx.channels(); z++) {
                        frameIdx.putDouble(new int[]{y, x, z}, y + x + z);
                    }
                }
            }
        }

        @Override
        public void start() throws Exception {
            if(shouldThrowAtStart) {
                throw new FrameGrabber.Exception("Throwing on start was enabled!");
            }
        }

        @Override
        public void stop() throws Exception {

        }

        @Override
        public void trigger() throws Exception {

        }

        @Override
        public Frame grab() throws Exception {
            return frame;
        }

        @Override
        public void release() throws Exception {
            this.frameIdx.release();
        }

        public void setShouldThrowAtStart(boolean shouldThrowAtStart){
            this.shouldThrowAtStart = shouldThrowAtStart;
        }
    }

    class MockFrameGrabberFactory implements CameraSource.FrameGrabberFactory {
        private MockFrameGrabber frameGrabber = new MockFrameGrabber();
        @Override
        public FrameGrabber create(int deviceNumber) {
            return frameGrabber;
        }

        @Override
        public FrameGrabber create(String addressProperty) throws MalformedURLException {
            return frameGrabber;
        }
    }

    @Before
    public void setUp() throws Exception {
        this.injector = Guice.createInjector(new GRIPCoreModule());
        this.cameraSourceFactory = injector.getInstance(CameraSource.Factory.class);

        this.eventBus = new EventBus();
        class UnhandledExceptionWitness {
            @Subscribe
            public void onUnexpectedThrowableEvent(UnexpectedThrowableEvent event) {
                System.err.println(event.getMessage());
                event.getThrowable().printStackTrace();
            }
        }
        this.eventBus.register(new UnhandledExceptionWitness());
        this.mockFrameGrabberFactory = new MockFrameGrabberFactory();
        this.cameraSourceWithMockGrabber = new CameraSource(eventBus, mockFrameGrabberFactory, 0);
    }

    @After
    public void tearDown() throws Exception {
        mockFrameGrabberFactory.frameGrabber.release();
    }

    @Test(expected = IOException.class)
    public void testInvalidURLThrowsIOException() throws Exception {
        cameraSourceFactory.create("Not a URL at all!");
        fail("This test should have thrown an exception.");
    }

    @Test
    public void testCanStopAndStart() throws Exception {
        cameraSourceWithMockGrabber.start();
        assertTrue("The camera source was not started after calling start", cameraSourceWithMockGrabber.isStarted());
        cameraSourceWithMockGrabber.stop();
        assertFalse("The camera was not stopped after calling stop", cameraSourceWithMockGrabber.isStarted());
    }

    @Test(expected = IOException.class)
    public void testStartRethrowsIfFailure() throws Exception {
        mockFrameGrabberFactory.frameGrabber.setShouldThrowAtStart(true);
        cameraSourceWithMockGrabber.start();
        fail("Should have thrown an IOException");
    }

    @Test(expected = IllegalStateException.class)
    public void testStartingTwiceShouldThrowIllegalState() throws Exception {
        try {
            try {
                cameraSourceWithMockGrabber.start();
            } catch(RuntimeException e) {
                fail("This should not have failed");
            }
            cameraSourceWithMockGrabber.start();
        } finally {
            cameraSourceWithMockGrabber.stop();
        }
        fail("The test should have failed with an IllegalStateException");
    }

}