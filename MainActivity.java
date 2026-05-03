package com.smokensmoke.pos;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    EditText productName, price, cost, qty;
    TextView cartView, totalView, reportView;
    DBHelper dbHelper;
    ArrayList<CartItem> cart = new ArrayList<>();
    double total = 0;
    double profit = 0;

    static class CartItem {
        String product;
        int qty;
        double price;
        double cost;

        CartItem(String product, int qty, double price, double cost) {
            this.product = product;
            this.qty = qty;
            this.price = price;
            this.cost = cost;
        }

        double lineTotal() { return qty * price; }
        double lineProfit() { return qty * (price - cost); }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        productName = findViewById(R.id.productName);
        price = findViewById(R.id.price);
        cost = findViewById(R.id.cost);
        qty = findViewById(R.id.qty);
        cartView = findViewById(R.id.cartView);
        totalView = findViewById(R.id.totalView);
        reportView = findViewById(R.id.reportView);

        findViewById(R.id.addBtn).setOnClickListener(v -> addToSale());
        findViewById(R.id.completeBtn).setOnClickListener(v -> completeSale());
        findViewById(R.id.reportBtn).setOnClickListener(v -> showDailyReport());
    }

    void addToSale() {
        String p = productName.getText().toString().trim();
        if (p.isEmpty()) { toast("Enter product name / اكتب اسم المنتج"); return; }

        try {
            double salePrice = Double.parseDouble(price.getText().toString());
            double costPrice = Double.parseDouble(cost.getText().toString());
            int q = Integer.parseInt(qty.getText().toString());
            cart.add(new CartItem(p, q, salePrice, costPrice));
            productName.setText(""); price.setText(""); cost.setText(""); qty.setText("");
            refreshCart();
        } catch (Exception e) {
            toast("Check price, cost, and quantity / تأكد من السعر والكمية");
        }
    }

    void refreshCart() {
        StringBuilder sb = new StringBuilder();
        total = 0;
        profit = 0;

        for (CartItem item : cart) {
            sb.append(item.product)
              .append("  x").append(item.qty)
              .append("  = $").append(String.format(Locale.US, "%.2f", item.lineTotal()))
              .append("\n");
            total += item.lineTotal();
            profit += item.lineProfit();
        }

        cartView.setText(sb.length() == 0 ? "Cart is empty / السلة فاضية" : sb.toString());
        totalView.setText("TOTAL USD: $" + String.format(Locale.US, "%.2f", total));
    }

    void completeSale() {
        if (cart.isEmpty()) { toast("Cart is empty / السلة فاضية"); return; }

        String invoice = "INV-" + System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        for (CartItem item : cart) {
            db.execSQL(
                "INSERT INTO sales(invoice, product, qty, price, cost, total, profit, created_at) VALUES(?,?,?,?,?,?,?,?)",
                new Object[]{ invoice, item.product, item.qty, item.price, item.cost, item.lineTotal(), item.lineProfit(), date }
            );
        }

        new AlertDialog.Builder(this)
            .setTitle("Sale Completed / تم البيع")
            .setMessage("Invoice: " + invoice + "\nTotal: $" + String.format(Locale.US, "%.2f", total) + "\nProfit: $" + String.format(Locale.US, "%.2f", profit))
            .setPositiveButton("OK", null)
            .show();

        cart.clear();
        refreshCart();
    }

    void showDailyReport() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        Cursor c = db.rawQuery(
            "SELECT COUNT(DISTINCT invoice), SUM(total), SUM(profit) FROM sales WHERE created_at LIKE ?",
            new String[]{ today + "%" }
        );

        if (c.moveToFirst()) {
            int invoices = c.getInt(0);
            double sales = c.isNull(1) ? 0 : c.getDouble(1);
            double profits = c.isNull(2) ? 0 : c.getDouble(2);

            reportView.setText(
                "DAILY REPORT / التقرير اليومي\n\n" +
                "Invoices / الفواتير: " + invoices + "\n" +
                "Total Sales USD / المبيعات: $" + String.format(Locale.US, "%.2f", sales) + "\n" +
                "Profit USD / الربح: $" + String.format(Locale.US, "%.2f", profits)
            );
        }
        c.close();
    }

    void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
