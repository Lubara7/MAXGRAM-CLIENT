package io.github.lubara7.tgmax.telegram;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;

/** Keep history compatible with MaxGram builds that used cache schema 181. */
public final class TgMaxCacheMigration {
    private TgMaxCacheMigration() {}

    public static int migrate(SQLiteDatabase db, int version) throws Exception {
        if (version < 179 || version >= 181) return version;
        ensureCurrent(db);
        db.executeFast("PRAGMA user_version = 181").stepThis().dispose();
        return 181;
    }

    public static void ensureCurrent(SQLiteDatabase db) throws SQLiteException {
        if (hasColumn(db, "widget_state", "height")) {
            if (db.tableExists("maxgram_widget_state_180_backup")) throw new IllegalStateException("Widget backup already exists");
            db.executeFast("ALTER TABLE widget_state RENAME TO maxgram_widget_state_180_backup").stepThis().dispose();
        }
        db.executeFast("CREATE TABLE IF NOT EXISTS widget_state(uid INTEGER,mid INTEGER,local TEXT,PRIMARY KEY(uid,mid))").stepThis().dispose();
        db.executeFast("CREATE TABLE IF NOT EXISTS widget_heights(uid INTEGER,mid INTEGER,width INTEGER,height INTEGER,date INTEGER,anchor INTEGER default 0,PRIMARY KEY(uid,mid,width))").stepThis().dispose();
        if (!hasColumn(db, "widget_heights", "anchor")) db.executeFast("ALTER TABLE widget_heights ADD COLUMN anchor INTEGER default 0").stepThis().dispose();
        if (db.tableExists("maxgram_widget_state_180_backup")) {
            String local = hasColumn(db, "maxgram_widget_state_180_backup", "local") ? "local" : "NULL";
            db.executeFast("INSERT OR IGNORE INTO widget_state(uid,mid,local) SELECT uid,mid," + local + " FROM maxgram_widget_state_180_backup").stepThis().dispose();
        }
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) throws SQLiteException {
        SQLiteCursor cursor = db.queryFinalized("PRAGMA table_info(" + table + ")");
        try { while (cursor.next()) if (column.equals(cursor.stringValue(1))) return true; return false; }
        finally { cursor.dispose(); }
    }
}
