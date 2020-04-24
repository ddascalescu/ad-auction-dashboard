package com.comp2211.dashboard.viewmodel;

import com.comp2211.dashboard.Campaign;
import com.comp2211.dashboard.model.data.Demographics;
import com.comp2211.dashboard.model.data.Demographics.Demographic;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import de.saxsys.mvvmfx.ViewModel;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.imageio.ImageIO;

public class PrimaryViewModel implements ViewModel {

  // TODO: Refactor to service and inject...
  private ObservableList<String> averages;
  private ObservableList<Demographic> demographics;
  private ObservableList<Campaign> campaigns;
  private ObservableList<String> totals;

  private ObservableList<Series<String, Number>> averageChartData;
  private ObservableList<PieChart.Data> demographicsChartData;
  private ObservableList<Series<String, Number>> totalMetricChartData;

  private StringProperty totalClickCost = new SimpleStringProperty("");
  private StringProperty totalImpresCost = new SimpleStringProperty("");
  private StringProperty totalCost = new SimpleStringProperty("");
  private StringProperty clickThroughRateText = new SimpleStringProperty("");
  private StringProperty bounceRateText = new SimpleStringProperty("");
  private StringProperty conversionUniquesText = new SimpleStringProperty("");

  private StringProperty totalImpressionsText = new SimpleStringProperty("");
  private StringProperty totalClicksText = new SimpleStringProperty("");
  private StringProperty totalUniquesText = new SimpleStringProperty("");
  private StringProperty totalBouncesText = new SimpleStringProperty("");
  private StringProperty totalConversionsText = new SimpleStringProperty("");


  private StringProperty selectedAverage = new SimpleStringProperty("");
  private StringProperty selectedTotals = new SimpleStringProperty("");
  private ObjectProperty<Demographic> selectedDemographic = new SimpleObjectProperty<Demographic>();
  private ObjectProperty<Campaign> selectedCampaign = new SimpleObjectProperty<Campaign>();

  private final String avgCostClick = "Average Cost of Click";
  private final String avgCostImpr = "Average Cost of Impression";
  private final String avgCostAcq = "Average Cost of Acquisition";

  private final String totalImpressions = "Impressions";
  private final String totalClicks = "Clicks";
  private final String totalUniques = "Uniques";
  private final String totalBounces = "Bounces";
  private final String totalConversions = "Conversions";

  private final ExtensionFilter pngFilter = new ExtensionFilter("PNG File (*.png)", "*.png");
  private final ExtensionFilter pdfFilter = new ExtensionFilter("PDF File (*.pdf)","*.pdf");

  public void initialize() {
    campaigns = FXCollections.observableArrayList();
    averages = FXCollections.observableArrayList();
    demographics = FXCollections.observableArrayList();
    averageChartData = FXCollections.observableArrayList();
    demographicsChartData = FXCollections.observableArrayList();
    totals = FXCollections.observableArrayList();
    totalMetricChartData = FXCollections.observableArrayList();


    campaigns.addAll(Campaign.getCampaigns());
    averages.addAll(avgCostClick, avgCostImpr, avgCostAcq);
    demographics.addAll(Demographics.Demographic.values());
    totals.addAll(totalImpressions, totalClicks, totalUniques, totalBounces, totalConversions);

    setupCampaignSelector();

    new Thread() {
      public void run() {
        selectedCampaign.getValue().cacheData();
        //TODO Change to load data over time: check campaign.loadData(limit, offset);
        Platform.runLater(new Runnable() {
          public void run() {
            updateTotalMetrics();
            updateTotalCosts();
            updateBounceRateDefault();
            setupDemographicSelector();
            setupAverageSelector();
            setUpTotalsSelector();
          }
        });
      }
    }.start();
  }



  public ObservableList<Campaign> campaignsList() {
    return campaigns;
  }

  public ObservableList<String> totalList() {return totals;}

  public ObservableList<String> averagesList() {
    return averages;
  }

  public ObservableList<Demographic> demographicsList() {
    return demographics;
  }

  public ObservableList<Series<String, Number>> averageChartData() {
    return averageChartData;
  }

  public ObservableList<Series<String, Number>> totalMetricChartData() {return totalMetricChartData;}

  public ObservableList<PieChart.Data> demographicsChartData() {
    return demographicsChartData;
  }

  public StringProperty selectedAverageProperty() {
    return selectedAverage;
  }

  public StringProperty selectedTotalMetricProperty() { return selectedTotals;}

  public ObjectProperty<Demographic> selectedDemographicProperty() {
    return selectedDemographic;
  }

  public ObjectProperty<Campaign> selectedCampaignProperty() {
    return selectedCampaign;
  }

  public StringProperty totalClickCostProperty() {
    return totalClickCost;
  }

  public StringProperty totalImpresCostProperty() {
    return totalImpresCost;
  }

  public StringProperty totalCostProperty() {
    return totalCost;
  }

  public StringProperty clickThroughRateTextProperty() {
    return clickThroughRateText;
  }

  public StringProperty bounceRateTextProperty() {
    return bounceRateText;
  }

  public StringProperty getBounceRateText(){return bounceRateText;}

  public StringProperty getConversionUniquesText(){return totalConversionsText;}

  public StringProperty getTotalImpressionsText() { return totalImpressionsText; }

  public StringProperty getTotalClicksText() { return totalClicksText;}

  public StringProperty getTotalUniquesText() { return totalUniquesText;}

  public StringProperty getTotalBouncesText() { return totalBouncesText;}

  public StringProperty getTotalConversionsText() { return totalConversionsText;}


  private void updateTotalCosts() {
    totalClickCost.setValue("£" + selectedCampaign.getValue().getTotalClickCost().setScale(2, RoundingMode.CEILING).toPlainString());
    totalImpresCost.setValue("£" + selectedCampaign.getValue().getTotalImpressionCost().setScale(2, RoundingMode.CEILING).toPlainString());
    totalCost.setValue("£" + selectedCampaign.getValue().getTotalCost().setScale(2, RoundingMode.CEILING).toPlainString());
    clickThroughRateText.setValue(selectedCampaign.getValue().getClickThroughRate().setScale(2, RoundingMode.CEILING).toPlainString() + "%");
  }

  private void updateTotalMetrics(){

    totalImpressionsText.setValue("32121");
    totalClicksText.setValue("99833");
    totalUniquesText.setValue("12002");
    totalBouncesText.setValue("11143");
    totalConversionsText.setValue("9733");
  }

  private void updateBounceRateDefault() {
    updateBounceRateByPages((byte) 1);
  }

  private void updateBounceRateByTime(long maxSeconds, boolean allowInf) {
    bounceRateText.setValue(selectedCampaign.getValue().getBounceRateByTime(maxSeconds, allowInf).setScale(2, RoundingMode.CEILING).toPlainString() + "%");
  }

  private void updateBounceRateByPages(byte maxPages) {
    bounceRateText.setValue(selectedCampaign.getValue().getBounceRateByPages(maxPages).setScale(2, RoundingMode.CEILING).toPlainString() + "%");
  }

  private void setupCampaignSelector() {
    selectedCampaign.addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        Optional<Campaign> matchingCampaign = Campaign.getCampaigns().stream().filter(newVal::equals).findFirst();
        matchingCampaign.ifPresent(cam -> selectedCampaign.setValue(cam));
      } else {
        selectedCampaign.setValue(Campaign.getCampaigns().get(0));
      }
    });
    selectedCampaign.setValue(Campaign.getCampaigns().get(0));
  }

  private void setupAverageSelector() {
    selectedAverage.addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        Optional<String> matchingAverage = averages.stream().filter(newVal::equals).findFirst();
        matchingAverage.ifPresent(avg -> selectedAverage.setValue(avg));
      } else {
        selectedAverage.setValue(avgCostClick);
      }
      if (selectedAverage.getValue().equals(avgCostClick)) {
        updateLineChartData(selectedCampaign.getValue().getDatedClickCostAverages());
      } else if (selectedAverage.getValue().equals(avgCostImpr)) {
        updateLineChartData(selectedCampaign.getValue().getDatedImpressionCostAverages());
      } else if (selectedAverage.getValue().equals(avgCostAcq)) {
        updateLineChartData(selectedCampaign.getValue().getDatedAcquisitionCostAverages());
      }
    });
    selectedAverage.setValue(avgCostClick);
  }

  private void setUpTotalsSelector(){

    selectedTotals.addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        Optional<String> matchingAverage = totals.stream().filter(newVal::equals).findFirst();
        matchingAverage.ifPresent(avg -> selectedTotals.setValue(avg));
      } else {
        selectedTotals.setValue(totalImpressions);
      }
      if (selectedAverage.getValue().equals(totalImpressions)) {
        //Update Total Metrics Graph with total impressions over time
        //use method updateTotalMetricLineChartData();

      } else if (selectedAverage.getValue().equals(totalClicks)) {
        //Update Total Metrics Graph with total clicks over time
        //use method updateTotalMetricLineChartData();

      } else if (selectedAverage.getValue().equals(totalUniques)) {
        //Update Total Metrics Graph with total uniques over time
        //use method updateTotalMetricLineChartData();

      } else if (selectedAverage.getValue().equals(totalBounces)) {
        //Update Total Metrics Graph with total bounces over time
        //use method updateTotalMetricLineChartData();

      } else if (selectedAverage.getValue().equals(totalConversions)) {
        //Update Total Metrics Graph with total conversions over time
        //use method updateTotalMetricLineChartData();

      }
    });
    selectedTotals.setValue(totalImpressions);
  }

  private void setupDemographicSelector() {
    selectedDemographic.addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        Optional<Demographic> matchingDemographic = Stream.of(Demographics.Demographic.values()).filter(newVal::equals).findFirst();
        matchingDemographic.ifPresent(dem -> selectedDemographic.setValue(dem));
      } else {
        selectedDemographic.setValue(Demographic.Gender);
      }
      updatePieChartData(selectedCampaign.getValue().getPercentageMap(selectedDemographic.getValue()));
    });
    selectedDemographic.setValue(Demographic.Gender);
  }

  private void updatePieChartData(HashMap<String, BigDecimal> dataMap) {
    demographicsChartData.clear();
    for (Entry<String, BigDecimal> entry : dataMap.entrySet()) {
      PieChart.Data pd = new PieChart.Data(entry.getKey(), entry.getValue().doubleValue());
      pd.setName(pd.getName() + " "+ entry.getValue().setScale(1, RoundingMode.HALF_UP).doubleValue() + "%");
      demographicsChartData.add(pd);
    }
  }

  private void updateLineChartData(HashMap<String, BigDecimal> dataMap) {
    averageChartData.clear();
    Series<String, Number> s = new Series<>();
    s.setName(selectedCampaign.getValue().toString());
    for (Entry<String, BigDecimal> entry : dataMap.entrySet()) {
      Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue().doubleValue());
      data.setNode(new ChartPointLabel(data.getYValue().toString()));
      s.getData().add(data);
    }
    averageChartData.add(s);
  }

  private void updateTotalMetricLineChartData (HashMap<String, BigDecimal> dataMap) {

  }

  /**
   * Saves given list of charts into pdf file
   * @param charts list of charts to save
   * @return true if save successful, false if fail
   */
  public boolean saveCharts(List<Chart> charts){
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().addAll(pdfFilter);
    File file = fileChooser.showSaveDialog(null);

    if (file == null) {
      return false;
    }
    saveChartAsPDF(charts, file);

    return true;
  }

  /**
   * Saves a single chart to either pdf or png (user chooses)
   * @param chart chart to save
   * @return true if save successful, false if fail
   */
  public boolean saveChart(Chart chart) {

    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().addAll(pngFilter, pdfFilter);
    File file = fileChooser.showSaveDialog(null);

    if (file == null) {
      return false;
    }

    if (fileChooser
        .getSelectedExtensionFilter()
        .getDescription()
        .equals(pngFilter.getDescription())) {
      saveChartAsPNG(chart, file);
    }
    else if (fileChooser.getSelectedExtensionFilter().getDescription().equals((pdfFilter.getDescription()))){
      saveChartAsPDF(chart, file);
    }
    return true;
  }


  /**
   * Method for saving as png
   * @param chart chart to save
   * @param file file name/location
   * @return true if save successful, false if fail
   */
  private boolean saveChartAsPNG(Chart chart, File file){
    WritableImage writableImage = chart.snapshot(new SnapshotParameters(), null);
    BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(writableImage, null );
    try {
      ImageIO.write(bufferedImage, "png", file);
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  /**
   * Method for saving given list of charts as pdf
   * @param charts list of charts to save
   * @param file file name/location
   * @return true if save successful, false if fail
   */
  public boolean saveChartAsPDF(List<Chart> charts, File file) {
    Document doc = new Document();
    PdfWriter pdfWriter = null;
    PdfContentByte pdfContentByte = null;

    try{
      if (!file.getName().endsWith(".pdf")) {
        file = new File(file, file.getName() + ".pdf");
      }
      pdfWriter = PdfWriter.getInstance(doc, new FileOutputStream(file));
      pdfContentByte = new PdfContentByte(pdfWriter);
    }catch (FileNotFoundException | DocumentException e){
      e.printStackTrace();
      return false;
    }

    if(pdfWriter == null | pdfContentByte == null){
      return false;
    }

    pdfWriter.open();
    doc.open();
    for (Chart chart : charts) {
      WritableImage writableImage = chart.snapshot(new SnapshotParameters(), null);
      BufferedImage bufferedImage =
          javafx.embed.swing.SwingFXUtils.fromFXImage(writableImage, null);
      try {
        Image image = com.itextpdf.text.Image.getInstance(pdfContentByte, bufferedImage, 1);
        image.scaleToFit(PageSize.A5);
        doc.add(image);
      } catch (DocumentException | IOException e) {
        e.printStackTrace();
        return false;
      }
    }
    doc.close();
    pdfWriter.close();
    return true;
    }

  /**
   * Method for saving a given chart as pdf
   * @param chart chart to save
   * @param file file name/location
   * @return true if save successful, false if fail
   */
  private boolean saveChartAsPDF(Chart chart, File file){
    return saveChartAsPDF(Arrays.asList(chart), file);
  }


}

final class ChartPointLabel extends StackPane {
  public ChartPointLabel(String value) {
    setPrefSize(12, 12);

    final Label label = new Label(value);
    label.getStyleClass().addAll("default-color0", "chart-line-symbol", "chart-series-line");
    label.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

    label.setTextFill(Color.DARKGRAY);
    label.setMinSize(Label.USE_PREF_SIZE, Label.USE_PREF_SIZE);

    setOnMouseEntered(new EventHandler<MouseEvent>() {
      @Override public void handle(MouseEvent e) {
        getChildren().setAll(label);
        label.setTranslateY(e.getY()+24);
        toFront();
      }
    });
    setOnMouseExited(new EventHandler<MouseEvent>() {
      @Override public void handle(MouseEvent e) {
        getChildren().clear();
      }
    });
  }
}
