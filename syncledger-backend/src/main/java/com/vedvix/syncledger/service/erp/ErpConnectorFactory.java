package com.vedvix.syncledger.service.erp;

import com.vedvix.syncledger.exception.BadRequestException;
import com.vedvix.syncledger.model.ErpType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that resolves the correct {@link ErpConnector} for a given ERP type.
 * All connectors are auto-discovered via Spring DI.
 *
 * @author vedvix
 */
@Component
public class ErpConnectorFactory {

    private final Map<ErpType, ErpConnector> connectors = new EnumMap<>(ErpType.class);

    public ErpConnectorFactory(List<ErpConnector> connectorList) {
        for (ErpConnector connector : connectorList) {
            connectors.put(connector.getErpType(), connector);
        }
    }

    /**
     * Get the connector for the given ERP type.
     *
     * @throws BadRequestException if not implemented
     */
    public ErpConnector getConnector(ErpType erpType) {
        ErpConnector connector = connectors.get(erpType);
        if (connector == null) {
            throw new BadRequestException(erpType.getDisplayName()
                    + " integration is not yet implemented. Coming soon!");
        }
        return connector;
    }

    /**
     * Check if a connector is available for the given ERP type.
     */
    public boolean hasConnector(ErpType erpType) {
        return connectors.containsKey(erpType);
    }
}
