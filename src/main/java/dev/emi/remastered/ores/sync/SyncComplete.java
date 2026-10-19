/*
 * Copyright (c) Abbie, licensed under LGPL-3.0-only; modified for EMI Remastered.
 */
package dev.emi.remastered.ores.sync;

/**
 * Last packet of an ore sync. The client only uses the data of a sync when this marker matches the start packet and
 * every batch arrived.
 *
 * @param batchCount number of batches the server sent, repeated from the start packet as a cross check
 */
public record SyncComplete(int syncId, int batchCount) {
}
