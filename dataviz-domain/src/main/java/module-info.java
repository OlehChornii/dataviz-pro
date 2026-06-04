module dataviz.domain {
    requires java.logging;

    exports com.dataviz.common.config;
    exports com.dataviz.common.event;
    exports com.dataviz.common.thread;
    exports com.dataviz.di;
    exports com.dataviz.di.annotation;
    exports com.dataviz.domain.chart;
    exports com.dataviz.domain.dashboard;
    exports com.dataviz.domain.filter;
    exports com.dataviz.domain.model;

    // Allow testing via reflection
    opens com.dataviz.domain.model to org.junit.platform.commons;
    opens com.dataviz.domain.filter to org.junit.platform.commons;
    opens com.dataviz.di to org.junit.platform.commons;
}
