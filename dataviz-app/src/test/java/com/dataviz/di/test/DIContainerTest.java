package com.dataviz.di.test;

import com.dataviz.common.event.DatasetObservable;
import com.dataviz.common.event.EventBus;
import com.dataviz.di.container.DIContainer;
import com.dataviz.di.exception.*;
import com.dataviz.di.annotation.Component;
import com.dataviz.di.annotation.Inject;
import com.dataviz.di.annotation.Service;
import com.dataviz.di.annotation.Singleton;
import com.dataviz.service.load.DataLoadService;
import com.dataviz.ui.presenter.ImportPresenter;
import com.dataviz.ui.view.IImportView;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DIContainerTest {

    private DIContainer container;

    interface Greeter { String greet(String name); }

    @Component
    @Singleton
    static class HelloGreeter implements Greeter {
        @Override public String greet(String name) { return "Hello, " + name + "!"; }
    }

    @Component
    static class UpperGreeter implements Greeter {
        @Override public String greet(String name) { return "HELLO, " + name.toUpperCase() + "!"; }
    }

    interface MessageSender { void send(String msg); }

    @Service
    @Singleton
    static class NotificationService {
        private final Greeter greeter;
        private final MessageSender sender;

        @Inject
        NotificationService(Greeter greeter, MessageSender sender) {
            this.greeter = greeter;
            this.sender  = sender;
        }

        public void notify(String name) {
            sender.send(greeter.greet(name));
        }
    }

    @BeforeEach
    void setUp() {
        container = new DIContainer();
    }

    @Test
    @DisplayName("Singleton: один і той самий екземпляр повертається двічі")
    void testSingletonScope() {
        container.register(HelloGreeter.class);
        Greeter g1 = container.resolve(Greeter.class);
        Greeter g2 = container.resolve(Greeter.class);
        assertSame(g1, g2, "Singleton must return the same instance");
    }

    @Test
    @DisplayName("Constructor Injection: всі залежності впроваджуються")
    void testConstructorInjection() {
        MessageSender mockSender = mock(MessageSender.class);

        container.register(HelloGreeter.class);
        container.registerInstance(MessageSender.class, mockSender);
        container.register(NotificationService.class);

        NotificationService svc = container.resolve(NotificationService.class);
        svc.notify("DataViz");

        verify(mockSender).send("Hello, DataViz!");
    }

    @Test
    @DisplayName("Named: вибір конкретної реалізації за іменем")
    void testNamedResolution() {
        container.register(HelloGreeter.class,  "hello");
        container.register(UpperGreeter.class,  "upper");

        Greeter hello = container.resolveNamed("hello", Greeter.class);
        Greeter upper = container.resolveNamed("upper", Greeter.class);

        assertEquals("Hello, Alice!", hello.greet("Alice"));
        assertEquals("HELLO, ALICE!", upper.greet("Alice"));
    }

    @Component static class A { @Inject B b; }
    @Component static class B { @Inject A a; }

    @Test
    @DisplayName("Circular Dependency: контейнер кидає виняток")
    void testCircularDependencyDetected() {
        container.register(A.class);
        container.register(B.class);

        assertThrows(CircularDependencyException.class, () -> container.resolve(A.class));
    }

    @Test
    @DisplayName("Unregistered: DIException при відсутньому компоненті")
    void testUnregisteredThrows() {
        assertThrows(DIException.class, () -> container.resolve(Greeter.class));
    }

    @Test
    @DisplayName("RegisterInstance: зовнішній об'єкт вирішується коректно")
    void testRegisterInstance() {
        Greeter customGreeter = name -> "Greetings, " + name;
        container.registerInstance(Greeter.class, customGreeter);

        Greeter resolved = container.resolve(Greeter.class);
        assertSame(customGreeter, resolved);
        assertEquals("Greetings, World", resolved.greet("World"));
    }

    @Test
    @DisplayName("Presenter тестується з Mock View (без JavaFX)")
    void testPresenterWithMockView() {
        IImportView         mockView       = mock(IImportView.class);
        DataLoadService     mockService    = mock(DataLoadService.class);
        EventBus            mockBus        = mock(EventBus.class);
        DatasetObservable   mockObservable = mock(DatasetObservable.class);

        ImportPresenter presenter = new ImportPresenter(mockService, mockBus, mockObservable);
        presenter.attachView(mockView);

        when(mockView.getSelectedFilePath()).thenReturn(null);
        presenter.onStartImportClicked();

        verify(mockView).showError(
                eq("Файл не вибрано"),
                anyString()
        );
        verify(mockService, never()).loadAsync(any(), any(), any());
    }
}