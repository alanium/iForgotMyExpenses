package com.example.iforgotmyexpenses;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "newDB.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Obtener el nombre de la tabla mensual
        String tableName = getMonthlyTableName();

        // Crear la tabla con el nombre obtenido
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + tableName + " (id_expenses INTEGER PRIMARY KEY AUTOINCREMENT, description TEXT, amount INTEGER, amount_total INTEGER, date TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Aquí puedes realizar las actualizaciones de la base de datos si cambia la versión
        // Por ejemplo, puedes eliminar y volver a crear la tabla

        String tableName = getMonthlyTableName();
        String dropTableQuery = "DROP TABLE IF EXISTS " + tableName;
        db.execSQL(dropTableQuery);
        onCreate(db);
    }

    public String getMonthlyTableName() {
        DateFormat dateFormat = new SimpleDateFormat("MMMM_yyyy", Locale.getDefault());
        Date date = new Date();
        return "expenses_" + dateFormat.format(date);
    }
}
