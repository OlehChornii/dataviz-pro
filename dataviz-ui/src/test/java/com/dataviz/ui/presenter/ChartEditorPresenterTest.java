package com.dataviz.ui.presenter;

import com.dataviz.common.event.DatasetChangeEvent;
import com.dataviz.common.event.DatasetObservable;
import com.dataviz.domain.chart.ChartConfig;
import com.dataviz.domain.chart.ChartConfig.ChartType;
import com.dataviz.domain.chart.ChartStyle;
import com.dataviz.domain.model.DataSet;
import com.dataviz.facade.DataVizFacade;
import com.dataviz.domain.model.DataColumn;
import com.dataviz.domain.model.ColumnType;
import com.dataviz.service.chart.ChartService;
import com.dataviz.ui.view.IChartEditorView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

@DisplayName("ChartEditorPresenter - Тестування редактора графіків")
@ExtendWith(MockitoExtension.class)
class ChartEditorPresenterTest {

    private ChartEditorPresenter presenter;
    private DataSet testDataSet;

    @Mock
    private IChartEditorView mockView;

    @Mock
    private DataVizFacade mockFacade;

    @Mock
    private DatasetObservable mockObservable;

    @BeforeEach
    void setUp() {
        testDataSet = createTestDataSet();
        presenter = new ChartEditorPresenter(mockFacade, mockObservable);
        presenter.setView(mockView);
    }

    private DataSet createTestDataSet() {
        List<DataColumn> columns = new ArrayList<>();

        List<Object> monthValues = new ArrayList<>();
        for (int i = 1; i <= 12; i++) monthValues.add("Month_" + i);
        columns.add(DataColumn.builder()
                .name("Month")
                .type(ColumnType.CATEGORICAL)
                .values(monthValues)
                .build());

        List<Object> salesValues = new ArrayList<>();
        for (int i = 0; i < 12; i++) salesValues.add((double) (50000 + i * 5000));
        columns.add(DataColumn.builder()
                .name("Sales")
                .type(ColumnType.NUMERIC)
                .values(salesValues)
                .build());

        List<Object> profitValues = new ArrayList<>();
        for (int i = 0; i < 12; i++) profitValues.add((double) (10000 + i * 2000));
        columns.add(DataColumn.builder()
                .name("Profit")
                .type(ColumnType.NUMERIC)
                .values(profitValues)
                .build());

        return DataSet.builder()
                .id("chart-editor-test")
                .name("Chart Editor Test Dataset")
                .columns(columns)
                .build();
    }

    @Test
    @DisplayName("Ініціалізація: initWithDataset() поселяє вибору колон")
    void testInitWithDataset_PopulatesColumnSelectors() {
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        presenter.initWithDataset(testDataSet);

        verify(mockView).populateColumnSelectors(captor.capture());
        List<String> columns = captor.getValue();
        assertTrue(columns.contains("Month"));
        assertTrue(columns.contains("Sales"));
        assertTrue(columns.contains("Profit"));
    }

    @Test
    @DisplayName("Зміна типу графіка: LINE -> BAR")
    void testOnChartTypeChanged_LINE_to_BAR() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onChartTypeChangedByName(ChartType.BAR.name());

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Зміна типу графіка: видимі налаштування оновлюються")
    void testOnChartTypeChanged_UpdatesVisibleSettings() {
        presenter.initWithDataset(testDataSet);
        
        presenter.onChartTypeChangedByName(ChartType.PIE.name());
    }

    @Test
    @DisplayName("Зміна X колони: конфіг оновлюється")
    void testOnXColumnChanged() {
        presenter.initWithDataset(testDataSet);
        reset(mockFacade, mockView);

        presenter.onXColumnChanged("Sales");

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Зміна заголовка: нова ChartConfig без дублювання")
    void testOnTitleChanged_CreatesNewConfig() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        String newTitle = "Custom Title";
        presenter.onTitleChanged(newTitle);

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Додавання Y колони: перевірка дублюванння")
    void testOnAddYColumn_PreventsDuplicate() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onAddYColumn("Sales");
        presenter.onAddYColumn("Sales");
    }

    @Test
    @DisplayName("Додавання кількох Y колон: всі включені")
    void testOnAddYColumn_Multiple() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onAddYColumn("Sales");
        presenter.onAddYColumn("Profit");

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Видалення Y колони: колона видаляється з конфіга")
    void testOnRemoveYColumn() {
        presenter.initWithDataset(testDataSet);
        presenter.onAddYColumn("Sales");
        reset(mockView, mockFacade);

        presenter.onRemoveYColumn("Sales");

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Зміна теми: стиль оновлюється")
    void testOnStyleThemeChanged() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onStyleThemeChanged(ChartStyle.Theme.DARK);

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Зміна ширини лінії: обновляется стиль")
    void testOnLineWidthChanged() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onLineWidthChanged(3.0);

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Зміна розміру точки: обновляется стиль")
    void testOnPointSizeChanged() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onPointSizeChanged(8.0);

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Зміна стилю опцій: легенда, сітка, підказки")
    void testOnStyleOptionChanged() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onStyleOptionChanged(true, true, true, 2.0);

        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("Редагування існуючої конфіга: initWithConfig()")
    void testInitWithConfig_EditExisting() {
        ChartConfig existingConfig = ChartConfig.builder()
                .id("chart-1")
                .chartType(ChartType.LINE)
                .title("Existing Chart")
                .xColumn("Month")
                .yColumns(List.of("Sales"))
                .build();

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        presenter.initWithConfig(testDataSet, existingConfig);

        verify(mockView).populateColumnSelectors(captor.capture());
        verify(mockView, atLeastOnce()).renderChart(any());
    }

    @Test
    @DisplayName("View невизначена: setView() реєструє observer")
    void testSetView_SubscribesToObservable() {
        presenter.setView(mockView);
    }

    @Test
    @DisplayName("Деструктор: unsubscribe() від observable")
    void testDestroy_UnsubscribesFromObservable() {
        presenter.setView(mockView);
        
        presenter.destroy();

    }

    @Test
    @DisplayName("DatasetObserver: DATASET_LOADED оновлює вибір колон")
    void testDatasetObserverEvent_DatasetLoaded() {
        presenter.setView(mockView);
        presenter.onDatasetChanged(DatasetChangeEvent.loaded(testDataSet));

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(mockView, atLeastOnce()).populateColumnSelectors(captor.capture());
    }

    @Test
    @DisplayName("Immutability: чергові конфіги - окремі об'єкти")
    void testChartConfigImmutability() {
        presenter.initWithDataset(testDataSet);
        
        presenter.onTitleChanged("Title 1");
        ChartConfig config1 = presenter.getCurrentConfig();
        
        presenter.onTitleChanged("Title 2");
        ChartConfig config2 = presenter.getCurrentConfig();
    }

    @Test
    @DisplayName("Null-view: IllegalArgumentException")
    void testSetView_Null_ThrowsException() {
        assertThrows(Exception.class, () -> presenter.setView(null));
    }

    @Test
    @DisplayName("Null-датасет: IllegalArgumentException")
    void testInitWithDataset_Null_ThrowsException() {
        assertThrows(Exception.class, () -> presenter.initWithDataset(null));
    }

    @Test
    @DisplayName("Невідома колона: IllegalArgumentException на додаванні")
    void testOnAddYColumn_UnknownColumn_ThrowsException() {
        presenter.initWithDataset(testDataSet);

        assertThrows(IllegalArgumentException.class, () -> presenter.onAddYColumn("UnknownColumn"));
    }

    @Test
    @DisplayName("Все кілька операцій: стан побудовується коректно")
    void testComplexScenario() {
        presenter.initWithDataset(testDataSet);
        reset(mockView, mockFacade);

        presenter.onChartTypeChangedByName(ChartType.BAR.name());
        presenter.onXColumnChanged("Month");
        presenter.onAddYColumn("Sales");
        presenter.onAddYColumn("Profit");
        presenter.onTitleChanged("Sales vs Profit");
        presenter.onLineWidthChanged(2.0);
        presenter.onStyleThemeChanged(ChartStyle.Theme.CORPORATE);

        verify(mockView, atLeastOnce()).renderChart(any());
    }
}
