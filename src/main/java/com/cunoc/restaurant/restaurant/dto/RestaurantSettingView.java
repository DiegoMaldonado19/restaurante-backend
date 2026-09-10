package com.cunoc.restaurant.restaurant.dto;

import com.cunoc.restaurant.restaurant.model.RestaurantSetting;

import java.math.BigDecimal;

public record RestaurantSettingView(
        Long       settingId,
        BigDecimal taxPercent,
        BigDecimal tipSuggestedPercent,
        BigDecimal pointsPerCurrencyUnit,
        BigDecimal currencyPerPoint)
{
    public static RestaurantSettingView from(RestaurantSetting setting)
    {
        return new RestaurantSettingView(
                setting.getSettingId(),
                setting.getTaxPercent(),
                setting.getTipSuggestedPercent(),
                setting.getPointsPerCurrencyUnit(),
                setting.getCurrencyPerPoint());
    }
}