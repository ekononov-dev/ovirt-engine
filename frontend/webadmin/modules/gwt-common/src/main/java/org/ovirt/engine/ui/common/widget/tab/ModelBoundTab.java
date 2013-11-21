package org.ovirt.engine.ui.common.widget.tab;

import org.ovirt.engine.ui.common.uicommon.model.ModelProvider;
import org.ovirt.engine.ui.common.uicommon.model.UiCommonInitEvent;
import org.ovirt.engine.ui.common.uicommon.model.UiCommonInitEvent.UiCommonInitHandler;
import org.ovirt.engine.ui.uicommonweb.models.EntityModel;
import org.ovirt.engine.ui.uicompat.Event;
import org.ovirt.engine.ui.uicompat.EventArgs;
import org.ovirt.engine.ui.uicompat.IEventListener;
import org.ovirt.engine.ui.uicompat.PropertyChangedEventArgs;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

public class ModelBoundTab extends SimpleTab implements HasHandlers {
    private final EventBus eventBus;

    public ModelBoundTab(final ModelBoundTabData tabData, AbstractTabPanel tabPanel, EventBus eventBus) {
        super(tabData, tabPanel);
        setAlign(tabData.getAlign());

        this.eventBus = eventBus;
        // Update tab accessibility
        setAccessible(tabData.getModelProvider().getModel().getIsAvailable());

        // Tab widgets are created as part of the corresponding TabView,
        // at this point UiCommonInitEvent has already been fired
        registerModelEventListeners(tabData.getModelProvider());

        // Add handler to be notified when UiCommon models are (re)initialized
        eventBus.addHandler(UiCommonInitEvent.getType(), new UiCommonInitHandler() {
            @Override
            public void onUiCommonInit(UiCommonInitEvent event) {
                setAccessible(tabData.getModelProvider().getModel().getIsAvailable());
                registerModelEventListeners(tabData.getModelProvider());
            }
        });
    }

    void registerModelEventListeners(final ModelProvider<? extends EntityModel> modelProvider) {
        modelProvider.getModel().getPropertyChangedEvent().addListener(new IEventListener() {
            @Override
            public void eventRaised(Event ev, Object sender, EventArgs args) {
                PropertyChangedEventArgs pcArgs = (PropertyChangedEventArgs) args;

                // Update tab accessibility when 'IsAvailable' property changes
                if ("IsAvailable".equals(pcArgs.propertyName)) { //$NON-NLS-1$
                    boolean isAvailable = modelProvider.getModel().getIsAvailable();
                    setAccessible(isAvailable);
                }
                TabAccessibleChangeEvent.fire(ModelBoundTab.this, ModelBoundTab.this);
            }
        });
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        eventBus.fireEvent(event);
    }
}
