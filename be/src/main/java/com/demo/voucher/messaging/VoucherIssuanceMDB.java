package com.demo.voucher.messaging;

import com.demo.voucher.service.VoucherFulfillmentService;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

@MessageDriven(activationConfig = {
    @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/jms/queue/VoucherIssuanceQueue"),
    @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
    @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge")
})
public class VoucherIssuanceMDB implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(VoucherIssuanceMDB.class.getName());

    @Inject
    private VoucherFulfillmentService fulfillmentService;

    @Override
    public void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage)) {
                LOGGER.warning("Received non-TextMessage on VoucherIssuanceQueue: " + message);
                return;
            }

            TextMessage textMessage = (TextMessage) message;
            String jsonPayload = textMessage.getText();
            String traceId = textMessage.getStringProperty("traceId");

            try (JsonReader reader = Json.createReader(new StringReader(jsonPayload))) {
                JsonObject json = reader.readObject();
                String claimCode = json.getString("claimCode");
                if (traceId == null && json.containsKey("traceId")) {
                    traceId = json.getString("traceId");
                }

                LOGGER.info(String.format("[Trace: %s] MDB received issuance event for claim: %s", traceId, claimCode));
                fulfillmentService.fulfillClaim(claimCode, traceId);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, "Failed to process JMS message in VoucherIssuanceMDB", e);
            throw new RuntimeException("Error processing issuance message", e);
        }
    }
}
