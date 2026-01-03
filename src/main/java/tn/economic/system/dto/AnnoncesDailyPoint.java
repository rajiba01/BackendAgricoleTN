package tn.economic.system.dto;

import java.math.BigDecimal;

public class AnnoncesDailyPoint {
  public String date;              // YYYY-MM-DD
  public String region;            // sfax|sahel|centre|nord|sud
  public int annonceCount;

  public BigDecimal annoncePriceMean;
  public BigDecimal annoncePriceMin;
  public BigDecimal annoncePriceMax;

  public BigDecimal annonceQualityMean;
  public BigDecimal annonceStockSum;
}