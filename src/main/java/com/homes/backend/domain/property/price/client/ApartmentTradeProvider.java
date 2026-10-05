package com.homes.backend.domain.property.price.client;

import com.homes.backend.domain.property.price.model.ApartmentTrade;

import java.time.YearMonth;
import java.util.List;

public interface ApartmentTradeProvider {
    List<ApartmentTrade> findMonthlyTrades(String sigunguCode, YearMonth contractMonth);
}
