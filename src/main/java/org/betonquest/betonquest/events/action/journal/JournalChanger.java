package org.betonquest.betonquest.events.action.journal;

import org.betonquest.betonquest.Journal;

/**
 * Defines changes to be done to a journal.
 */
public interface JournalChanger {
    /**
     * Apply the change to a journal.
     *
     * @param journal journal to change
     */
    void changeJournal(Journal journal);
}
