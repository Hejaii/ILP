package uk.ac.ed.inf;

import uk.ac.ed.inf.ilp.constant.OrderStatus;
import uk.ac.ed.inf.ilp.constant.OrderValidationCode;

public record JsonOrder(String orderNo, OrderStatus orderstatus, OrderValidationCode ordervalidationCode, int costInPence){
}
