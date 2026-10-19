package dev.emi.remastered.professions.trade;

/**
 * Identity of a trade for de-duplication: what is bought and what is sold. The parts are opaque keys supplied by the
 * caller (they only need value-based {@code equals}/{@code hashCode}); a missing second input is {@code null}.
 */
public record TradeKey(Object first, Object second, Object output) {
}
