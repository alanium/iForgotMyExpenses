package com.example.iforgotmyexpenses;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesDAO {
    private DatabaseHelper dbHelper;
    private String tableName;

    public ExpensesDAO(Context context) {
        dbHelper = new DatabaseHelper(context);
        tableName = dbHelper.getMonthlyTableName();
    }

    public void createExpensesDB(Expenses expenses) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("description", expenses.getDescription());
        values.put("amount", expenses.getAmount());
        values.put("date", getCurrentDate());


        db.insert(tableName, null, values);

        // Actualizar el total acumulado
        String updateQuery = "UPDATE " + tableName + " SET amount_total = (SELECT SUM(amount) FROM " + tableName + ")";
        db.execSQL(updateQuery);

        db.close();
    }

    private String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    public List<Expenses> getAllExpenses() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Expenses> expensesList = new ArrayList<>();

        String[] columns = {"id_expenses", "description", "amount", "amount_total", "date"};

        Cursor cursor = db.query(tableName, columns, null, null, null, null, null);
        while (cursor.moveToNext()) {
            int id_expenses = cursor.getInt(cursor.getColumnIndexOrThrow("id_expenses"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            int amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount"));
            int amountTotal = cursor.getInt(cursor.getColumnIndexOrThrow("amount_total"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            Expenses expenses = new Expenses();
            expenses.setId_expenses(id_expenses);
            expenses.setAmount(amount);
            expenses.setDescription(description);
            expenses.setAmount_total(amountTotal);
            expenses.setDate(date);

            expensesList.add(expenses);
        }

        cursor.close();
        db.close();

        return expensesList;
    }

    public void deleteExpensesDB(int id_expenses) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();

            // Obtener el monto del gasto a eliminar
            int deletedAmount = getAmountById(db, id_expenses);

            String whereClause = "id_expenses = ?";
            String[] whereArgs = {String.valueOf(id_expenses)};

            db.delete(tableName, whereClause, whereArgs);

            // Restar el monto eliminado al total acumulado
            String updateQuery = "UPDATE " + tableName + " SET amount_total = amount_total - " + deletedAmount;
            db.execSQL(updateQuery);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private int getAmountById(SQLiteDatabase db, int id_expenses) {
        int amount = 0;

        String[] columns = {"amount"};
        String selection = "id_expenses = ?";
        String[] selectionArgs = {String.valueOf(id_expenses)};

        Cursor cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount"));
        }

        cursor.close();

        return amount;
    }

    public int getTotalAmount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        int totalAmount = 0;

        String[] columns = {"amount_total"};
        Cursor cursor = db.query(tableName, columns, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            totalAmount = cursor.getInt(cursor.getColumnIndexOrThrow("amount_total"));
        }

        cursor.close();
        db.close();

        return totalAmount;
    }

    public List<String> getAllDescriptions() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<String> descriptions = new ArrayList<>();

        String[] columns = {"description"};

        Cursor cursor = db.query(tableName, columns, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            descriptions.add(description);
        }

        cursor.close();
        db.close();

        return descriptions;
    }

    public List<Expenses> getAllExpensesByTable(String table_Name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Expenses> expensesList = new ArrayList<>();

        String[] columns = {"id_expenses", "description", "amount", "amount_total", "date"};

        Cursor cursor = db.query(table_Name, columns, null, null, null, null, null);
        while (cursor.moveToNext()) {
            int id_expenses = cursor.getInt(cursor.getColumnIndexOrThrow("id_expenses"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            int amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount"));
            int amountTotal = cursor.getInt(cursor.getColumnIndexOrThrow("amount_total"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            Expenses expenses = new Expenses();
            expenses.setId_expenses(id_expenses);
            expenses.setAmount(amount);
            expenses.setDescription(description);
            expenses.setAmount_total(amountTotal);
            expenses.setDate(date);

            expensesList.add(expenses);
        }

        cursor.close();
        db.close();

        return expensesList;
    }

    public void updateExpensesDB(int id_expenses, String newAmount, String newDescription) {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabase();

            // Obtener el monto y descripción antiguos del gasto
            int oldAmount = getAmountById(db, id_expenses);
            String oldDescription = getDescriptionById(db, id_expenses);

            // Actualizar los valores del gasto
            ContentValues values = new ContentValues();
            values.put("amount", newAmount);
            values.put("description", newDescription);

            String whereClause = "id_expenses = ?";
            String[] whereArgs = {String.valueOf(id_expenses)};

            db.update(tableName, values, whereClause, whereArgs);

            // Actualizar el total acumulado
            int updatedAmount = Integer.parseInt(newAmount) - oldAmount;
            String updateQuery = "UPDATE " + tableName + " SET amount_total = amount_total + " + updatedAmount;
            db.execSQL(updateQuery);
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    private String getDescriptionById(SQLiteDatabase db, int id_expenses) {
        String description = "";

        String[] columns = {"description"};
        String selection = "id_expenses = ?";
        String[] selectionArgs = {String.valueOf(id_expenses)};

        Cursor cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
        }

        cursor.close();

        return description;
    }

    public Expenses getExpensesById(int selectedId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Expenses expenses = null;

        String[] columns = {"id_expenses", "description", "amount", "amount_total", "date"};
        String selection = "id_expenses = ?";
        String[] selectionArgs = {String.valueOf(selectedId)};

        Cursor cursor = db.query(tableName, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            int id_expenses = cursor.getInt(cursor.getColumnIndexOrThrow("id_expenses"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            int amount = cursor.getInt(cursor.getColumnIndexOrThrow("amount"));
            int amountTotal = cursor.getInt(cursor.getColumnIndexOrThrow("amount_total"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            expenses = new Expenses();
            expenses.setId_expenses(id_expenses);
            expenses.setAmount(amount);
            expenses.setDescription(description);
            expenses.setAmount_total(amountTotal);
            expenses.setDate(date);
        }

        cursor.close();
        db.close();

        return expenses;
    }

}
