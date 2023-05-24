package com.example.iforgotmyexpenses;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ExpensesDAO expensesDAO;
    private DatabaseHelper databaseHelper;

    private EditText input_amount;
    private AutoCompleteTextView description_amount;
    private ArrayAdapter<String> autoCompleteAdapter;
    private TextView show_amount;
    private Button bt_add;
    private ListView list_amount;
    private int selectedId = -1;
    private int selectedPosition = -1;
    private int totalAmount = 0;
    private Button bt_historial;
    private FloatingActionButton info_button;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDatabase();

        input_amount = findViewById(R.id.input_amount);
        description_amount = findViewById(R.id.description_amount);
        show_amount = findViewById(R.id.show_amount);
        bt_add = findViewById(R.id.bt_add);
        list_amount = findViewById(R.id.list_amount);
        bt_historial = findViewById(R.id.bt_historial);
        info_button = findViewById(R.id.info_button);

        // Obtén las descripciones de la tabla "descriptions" y crea el ArrayAdapter
        List<String> descriptions = expensesDAO.getAllDescriptions();
        autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, descriptions);

        // Establece el ArrayAdapter como el adaptador del AutoCompleteTextView
        description_amount.setAdapter(autoCompleteAdapter);
        description_amount.setThreshold(1); // Mostrar sugerencias a partir del primer carácter ingresado

        // Cambiar la dirección de despliegue hacia abajo
        description_amount.setDropDownHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        description_amount.setDropDownAnchor(R.id.description_amount);

        //update list
        loadExpensesList();

        //button ADD
        bt_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = input_amount.getText().toString().trim();
                String desc = description_amount.getText().toString().trim();

                if (input.isEmpty() || desc.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int inputToInt = Integer.parseInt(input);
                Expenses expenses = new Expenses(desc, inputToInt);

                // Actualizar el total de gastos
                expensesDAO.createExpensesDB(expenses);

                // Actualiza la lista
                loadExpensesList();

                //vacia los input
                description_amount.setText("");
                input_amount.setText("");

                // Actualizar el ArrayAdapter del AutoCompleteTextView
                List<String> updatedDescriptions = expensesDAO.getAllDescriptions();
                autoCompleteAdapter.clear();
                autoCompleteAdapter.addAll(updatedDescriptions);
                autoCompleteAdapter.notifyDataSetChanged();
            }
        });

        //List
        loadExpensesList();

        //button History
        bt_historial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryDialog();
            }
        });

        info_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Acción a realizar cuando se hace clic en el botón de envío de ideas
                // Por ejemplo, mostrar un cuadro de diálogo para ingresar y enviar una idea
                showIdeasDialog();
            }
        });

    }

    private void showIdeasDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enviar Idea o Cambio");

        // Inflar el diseño personalizado para el formulario de envío de correo electrónico
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ideas, null);
        builder.setView(dialogView);

        final EditText nameInput = dialogView.findViewById(R.id.name_input);
        final EditText ideaInput = dialogView.findViewById(R.id.idea_input);

        builder.setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameInput.getText().toString().trim();
                String idea = ideaInput.getText().toString().trim();

                // Validar los campos del formulario
                if (TextUtils.isEmpty(name)) {
                    showAlertDialog("Error", "Ingresa un nombre válido");
                } else if (TextUtils.isEmpty(idea)) {
                    showAlertDialog("Error", "Ingresa una idea o cambio válido");
                } else {
                    // Enviar el correo electrónico
                    sendEmail(name, idea);
                }
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void sendEmail(String name, String idea) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alan.g.dev@hotmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Nueva idea o cambio");
        intent.putExtra(Intent.EXTRA_TEXT, "Nombre: " + name + "\n\nIdea o cambio: " + idea);

        try {
            startActivity(Intent.createChooser(intent, "Enviar correo electrónico"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay aplicaciones de correo electrónico instaladas.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .show();
    }

    private void loadExpensesList() {
        List<Expenses> expensesList = expensesDAO.getAllExpenses();

        // Invierte el orden de la lista
        Collections.reverse(expensesList);

        ArrayAdapter<Expenses> adapter = new ArrayAdapter<Expenses>(MainActivity.this, android.R.layout.simple_list_item_2, android.R.id.text1, expensesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;

                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.list_item_layout, null);
                }

                Expenses expenses = expensesList.get(position);

                TextView textDescription = view.findViewById(R.id.text_description);
                TextView textDate = view.findViewById(R.id.text_date);
                TextView textAmount = view.findViewById(R.id.text_amount);

                textDescription.setText(expenses.getDescription());
                textDate.setText(expenses.getDate());

                // Formatea el monto en el formato deseado
                NumberFormat numberFormat = NumberFormat.getInstance();
                String formattedAmount = numberFormat.format(expenses.getAmount());

                textAmount.setText("$" + formattedAmount);

                return view;
            }
        };

        list_amount.setAdapter(adapter);

        list_amount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedId = expensesList.get(position).getId_expenses();
                selectedPosition = position;
                showOptionsDialog();
            }
        });

        // Recuperar y mostrar el total acumulado
        totalAmount = expensesDAO.getTotalAmount();

        // Formatea el total gastado en el formato deseado
        NumberFormat numberFormat = NumberFormat.getInstance();
        String formattedTotalAmount = numberFormat.format(totalAmount);

        show_amount.setText("$" + formattedTotalAmount);
    }

    private void showOptionsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Opciones")
                .setItems(new CharSequence[]{"Eliminar", "Editar", "Volver"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Eliminar
                                showDeleteConfirmationDialog();
                                break;

                            case 1: // Editar
                                showEditDialog();
                                break;

                            case 2: // Volver
                                break;
                        }
                    }
                })
                .show();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar");

        // Crear el layout personalizado para el diálogo de edición
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_expenses, null);
        final EditText editAmount = view.findViewById(R.id.edit_amount);
        final EditText editDescription = view.findViewById(R.id.edit_description);

        // Mostrar los valores actuales del gasto en los campos de edición
        if (selectedId != -1) {
            Expenses selectedExpense = expensesDAO.getExpensesById(selectedId);
            if (selectedExpense != null) {
                editAmount.setText(String.valueOf(selectedExpense.getAmount()));
                editDescription.setText(selectedExpense.getDescription());
            }
        }

        // Configurar los botones del diálogo
        builder.setPositiveButton("Guardar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Obtener los nuevos valores del gasto editado
                String newAmount = editAmount.getText().toString();
                String newDescription = editDescription.getText().toString();

                // Actualizar el gasto en la base de datos
                if (selectedId != -1) {
                    expensesDAO.updateExpensesDB(selectedId, newAmount, newDescription);
                    Toast.makeText(MainActivity.this, "Gasto editado correctamente", Toast.LENGTH_SHORT).show();
                    selectedId = -1;
                    loadExpensesList(); // Actualizar la lista después de editar el gasto
                }
            }
        });

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Cancelar la edición
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setView(view); // Establecer el diseño personalizado en el diálogo
        dialog.show();
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Eliminar")
                .setMessage("¿Estás seguro de que deseas eliminar este gasto?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Lógica para eliminar el gasto seleccionado
                        if (selectedId != -1) {
                            expensesDAO.deleteExpensesDB(selectedId);
                            Toast.makeText(MainActivity.this, "Gasto eliminado correctamente", Toast.LENGTH_SHORT).show();
                            selectedId = -1;
                            loadExpensesList(); // Actualizar la lista después de eliminar el gasto
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initDatabase();
    }

    private void initDatabase() {
        databaseHelper = new DatabaseHelper(MainActivity.this);
        databaseHelper.onCreate(databaseHelper.getWritableDatabase());
        expensesDAO = new ExpensesDAO(MainActivity.this);
    }

    private void showHistoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar tabla");

        // Obtén la lista de tablas disponibles (puedes obtenerla desde tu sistema de almacenamiento de datos)
        List<String> tableList = getTableList();

        // Modificar el formato de los nombres de tabla
        List<String> modifiedTableList = new ArrayList<>();
        for (String tableName : tableList) {
            String modifiedTableName = tableName.replace("expenses_", "");
            modifiedTableList.add(modifiedTableName);
        }

        // Configura el desplegable
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modifiedTableList);
        final Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);

        builder.setView(spinner);

        builder.setPositiveButton("Ver", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Obtiene la tabla seleccionada del desplegable
                int selectedPosition = spinner.getSelectedItemPosition();
                String selectedTableName = tableList.get(selectedPosition);

                // Muestra el contenido de la tabla seleccionada en el ListView
                showTableContent(selectedTableName);
            }
        });

        builder.setNeutralButton("Gráfico", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Obtiene la tabla seleccionada del desplegable
                int selectedPosition = spinner.getSelectedItemPosition();
                String selectedTableName = tableList.get(selectedPosition);

                // Obtener los gastos de la tabla seleccionada
                List<Expenses> expensesList = expensesDAO.getAllExpensesByTable(selectedTableName);

                // Crear un mapa para almacenar la suma de montos por descripción
                HashMap<String, Integer> expensesMap = new HashMap<>();

                // Calcular la suma de montos por descripción
                for (Expenses expense : expensesList) {
                    String description = expense.getDescription();
                    int amount = expense.getAmount();

                    if (expensesMap.containsKey(description)) {
                        int totalAmount = expensesMap.get(description);
                        totalAmount += amount;
                        expensesMap.put(description, totalAmount);
                    } else {
                        expensesMap.put(description, amount);
                    }
                }

                // Crear un objeto PieChart
                PieChart pieChart = new PieChart(getApplicationContext());

                // Crear una lista de entradas para el gráfico
                ArrayList<PieEntry> pieEntries = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : expensesMap.entrySet()) {
                    String description = entry.getKey();
                    int totalAmount = entry.getValue();
                    pieEntries.add(new PieEntry(totalAmount, description));
                }

                // Crear un conjunto de datos para el gráfico
                PieDataSet dataSet = new PieDataSet(pieEntries, "");

                // Personalizar colores de las secciones
                ArrayList<Integer> colors = new ArrayList<>();

                colors.add(Color.rgb(255, 204, 204));  // Rosa pastel
                colors.add(Color.rgb(204, 255, 204));  // Verde pastel
                colors.add(Color.rgb(204, 204, 255));  // Azul pastel
                colors.add(Color.rgb(255, 218, 185));  // Melocotón pastel
                colors.add(Color.rgb(255, 228, 181));  // Naranja pastel
                colors.add(Color.rgb(240, 230, 140));  // Amarillo pastel
                colors.add(Color.rgb(152, 251, 152));  // Verde claro pastel
                colors.add(Color.rgb(175, 238, 238));  // Turquesa pastel
                colors.add(Color.rgb(221, 160, 221));  // Lavanda pastel

                dataSet.setColors(colors);


                // Crear un objeto PieData con el conjunto de datos
                PieData pieData = new PieData(dataSet);

                // Personalizar las leyendas
                pieData.setDrawValues(true);  // Mostrar leyendas
                pieData.setValueFormatter(new PercentFormatter());  // Formatear los valores como porcentajes

                // Establecer los datos en el gráfico
                pieChart.setData(pieData);

                // Personalizar opciones del gráfico
                //pieChart.setCenterText("Gastos");  // Establecer un título central para el gráfico
                pieChart.setDrawEntryLabels(false);  // No mostrar etiquetas en las secciones del gráfico
                pieChart.getDescription().setEnabled(false);  // Deshabilitar la descripción del gráfico
                pieChart.getLegend().setEnabled(true);  // Habilitar la leyenda del gráfico
                pieChart.animateXY(1000, 1000);  // Agregar animaciones al gráfico (duración en milisegundos)
                dataSet.setValueTextColor(Color.BLACK);  // Color para los números dentro del gráfico
                dataSet.setValueTextSize(14f);  // Tamaño de los números dentro del gráfico

                // Personalizar opciones de la leyenda
                Legend legend = pieChart.getLegend();
                legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);  // Alinear la leyenda en la parte superior
                legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);  // Alinear la leyenda a la derecha
                legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);  // Mostrar la leyenda en forma vertical
                legend.setDrawInside(false);  // No dibujar la leyenda dentro del gráfico
                legend.setWordWrapEnabled(true); // Permitir ajuste de varias filas
                legend.setXEntrySpace(10f);  // Espacio horizontal entre las entradas de la leyenda
                legend.setYEntrySpace(10f);  // Espacio vertical entre las entradas de la leyenda
                legend.setTextColor(Color.WHITE);  // Color blanco para el texto de la leyenda
                legend.setTextSize(15f);  // Tamaño del texto de la leyenda



                // Crear un objeto ValueFormatter para formatear los valores como etiquetas personalizadas
                ValueFormatter valueFormatter = new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        // Convertir el valor a un entero
                        int intValue = (int) value;

                        return String.valueOf(intValue);
                    }
                };

                // Establecer el ValueFormatter en el objeto PieData
                pieData.setValueFormatter(valueFormatter);


                // Ajustar el tamaño del gráfico
                pieChart.setMinimumWidth(800);  // Ancho mínimo en píxeles
                pieChart.setMinimumHeight(800); // Altura mínima en píxeles

                // Crear un cuadro de diálogo personalizado
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                //dialogBuilder.setTitle("Gráfico de gastos");

                // Asignar la vista del gráfico al cuadro de diálogo
                dialogBuilder.setView(pieChart);

                // Mostrar el cuadro de diálogo
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
            }
        });



        builder.setNegativeButton("Cancelar", null);

        // Muestra el cuadro de diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private List<String> getTableList() {
        List<String> tableList = new ArrayList<>();

        // Obtén una instancia de la base de datos
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(getDatabasePath("newDB.db"), null);

        // Consulta para obtener los nombres de las tablas con el formato "expenses_mes_año"
        String query = "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'expenses_%'";
        Cursor cursor = db.rawQuery(query, null);

        // Recorre el cursor y agrega los nombres de las tablas a la lista
        while (cursor.moveToNext()) {
            String tableName = cursor.getString(0);
            tableList.add(tableName);
        }

        // Cierra el cursor y la base de datos
        cursor.close();
        db.close();

        return tableList;
    }

    private void showTableContent(String tableName) {
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(getDatabasePath("newDB.db"), null);

        // Consulta para obtener el contenido de la tabla seleccionada
        String query = "SELECT amount, description, date FROM " + tableName;
        Cursor cursor = db.rawQuery(query, null);

        // Obtener los índices de las columnas
        int amountIndex = cursor.getColumnIndex("amount");
        int descriptionIndex = cursor.getColumnIndex("description");
        int dateIndex = cursor.getColumnIndex("date");

        // Crear una lista de elementos en el formato "amount - description - date"
        List<String> itemList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                int amount = cursor.getInt(amountIndex);
                String description = cursor.getString(descriptionIndex);
                String date = cursor.getString(dateIndex);

                String item = "$ " + amount + " - " + description + " - " + date;
                itemList.add(item);
            } while (cursor.moveToNext());
        }

        // Cierra el cursor y la base de datos
        cursor.close();
        db.close();

        // Obtén el nombre de la tabla sin el prefijo "expenses_"
        String modifiedTableName = tableName.replace("expenses_", "");

        // Obtén el total gastado de la tabla seleccionada utilizando el método getTotalAmount()
        ExpensesDAO expensesDAO = new ExpensesDAO(MainActivity.this);
        int totalAmount = expensesDAO.getTotalAmount();

        // Crea el título del cuadro de diálogo con el nombre de la tabla y el total gastado
        String dialogTitle = modifiedTableName + " - total: $" + totalAmount;

        // Crea un ArrayAdapter para mostrar la lista en el ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                itemList
        );

        // Crea un AlertDialog y establece el ListView y el título como su vista personalizada
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(dialogTitle)
                .setAdapter(adapter, null)
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Acciones al hacer clic en Aceptar
                    }
                });

        // Muestra el cuadro de diálogo
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}