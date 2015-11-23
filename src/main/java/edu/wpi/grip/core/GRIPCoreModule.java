package edu.wpi.grip.core;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class GRIPCoreModule extends AbstractModule {
    private final EventBus eventBus = new EventBus();

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return this.eventBus;
    }

    @Provides
    @Singleton
    Pipeline providePipeline() {
        return new Pipeline(this.eventBus);
    }
}
