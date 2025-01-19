package uk.ac.ed.inf;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonDeserializer;
import uk.ac.ed.inf.ilp.data.CreditCardInformation;

import java.io.IOException;

/**
 * Custom JSON deserializer for {@link CreditCardInformation} objects.
 */
public class CardDeserializer extends JsonDeserializer<CreditCardInformation> {

    /**
     * Deserializes JSON content into a {@link CreditCardInformation} object.
     *
     * @param jsonParser              The JSON parser.
     * @param deserializationContext  The deserialization context.
     * @return The deserialized {@link CreditCardInformation} object.
     * @throws IOException        If an I/O error occurs during deserialization.
     * @throws JacksonException   If a Jackson-related exception occurs.
     */
    @Override
    public CreditCardInformation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JacksonException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        String creditCardNumber = node.get("creditCardNumber").asText();
        String creditCardExpiry = node.get("creditCardExpiry").asText();
        String cvv = node.get("cvv").asText();

        return new CreditCardInformation(creditCardNumber, creditCardExpiry, cvv);
    }
}


