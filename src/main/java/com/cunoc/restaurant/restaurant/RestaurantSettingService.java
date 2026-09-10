package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.restaurant.dto.RestaurantSettingView;
import com.cunoc.restaurant.restaurant.dto.UpdateSettingDTO;
import com.cunoc.restaurant.restaurant.model.RestaurantSetting;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La unica fila de configuracion del sistema (setting_id = 1, garantizado por el
 * CHECK y sembrada en V2). La leen customer (tasa de puntos, paso 2.5) y la
 * precuenta de billing; la escribe solo el administrador via PUT /settings.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantSettingService
{
    private static final long SINGLETON_ID = 1L;

    private final RestaurantSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public RestaurantSettingView get()
    {
        return RestaurantSettingView.from(findOrFail());
    }

    public RestaurantSettingView update(UpdateSettingDTO request)
    {
        var setting = findOrFail();
        setting.setTaxPercent(request.taxPercent());
        setting.setTipSuggestedPercent(request.tipSuggestedPercent());
        setting.setPointsPerCurrencyUnit(request.pointsPerCurrencyUnit());
        setting.setCurrencyPerPoint(request.currencyPerPoint());

        return RestaurantSettingView.from(setting);
    }

    private RestaurantSetting findOrFail()
    {
        return settingRepository.findById(SINGLETON_ID)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SETTING_NOT_FOUND,
                        "No hay configuracion del restaurante."));
    }
}