package com.lord.punishment.repositories;

import com.lord.punishment.Punishment;
import com.lord.punishment.PunishmentType;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PunishmentRepository {

    /**
     * Finds a specific punishment by its unique ID.
     *
     * @param punishmentId The unique ID of the punishment.
     * @return An Optional containing the punishment if found.
     */
    Optional<Punishment> findById(UUID punishmentId);

    /**
     * Finds all punishments (active and inactive) for a specific player.
     *
     * @param playerUuid The UUID of the player.
     * @return A Set of all punishments for the player.
     */
    Set<Punishment> findByPlayer(UUID playerUuid);

    /**
     * Finds all ACTIVE punishments of a specific type for a player.
     * This is the primary method to check if a player is currently banned or muted.
     *
     * @param playerUuid The UUID of the player.
     * @param type       The type of punishment to look for (e.g., BAN, MUTE).
     * @return A Set of active punishments of the specified type.
     */
    Set<Punishment> findActiveByType(UUID playerUuid, PunishmentType type);

    /**
     * Saves a punishment to the storage.
     *
     * @param punishment The punishment to save.
     */
    void save(Punishment punishment);

    /**
     * Deletes a punishment from the storage.
     * Note: In a real system, you might want to mark punishments as "pardoned"
     * instead of deleting them to keep audit logs. For our system, delete is fine for now.
     *
     * @param punishment The punishment to delete.
     */
    void delete(Punishment punishment);

}
