package com.yantriks.urbandatacomparator.validation;

import org.springframework.stereotype.Component;

@Component
public class UrbanConditionCheck {

    private Boolean actionMode;

    public Boolean getActionMode() {
        return actionMode;
    }

    public void setActionMode(Boolean actionMode) {
        this.actionMode = actionMode;
    }

    public boolean isActionModeCompareAndUpdate() {
        return true;
    }

}
