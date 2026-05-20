module dataviz.service {
    requires dataviz.domain;
    requires dataviz.data;

    requires java.sql;
    requires java.prefs;
    requires java.rmi;

    requires io;
    requires kernel;
    requires layout;

    exports com.dataviz.facade;
    exports com.dataviz.service.chart;
    exports com.dataviz.service.export;
    exports com.dataviz.service.filter;
    exports com.dataviz.service.load;
    exports com.dataviz.service.project;

    opens com.dataviz.service.chart;
    opens com.dataviz.service.export;
    opens com.dataviz.service.filter;
    opens com.dataviz.service.load;
    opens com.dataviz.service.project;
}