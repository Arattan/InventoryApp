package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

public class InventoryCursorAdapter extends CursorAdapter {


    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
// Inflate a list item view using the layout specified in list_item.xml
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = view.findViewById(R.id.product_name);
        TextView quantityTextView = view.findViewById(R.id.current_quantity);
        TextView priceTextView = view.findViewById(R.id.price);

        //Find sale button in each list item.
        Button saleButton = view.findViewById(R.id.sale_button);

        // Find the columns of  attributes that we're interested in
        final int idColumnIndex = cursor.getColumnIndex(InventoryEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);

        // Read the product attributes from the Cursor for the current product
        final int productId = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        String productQuantity = cursor.getString(quantityColumnIndex);
        String productPrice = cursor.getString(priceColumnIndex);

        // If the quantity is empty string or null, then use some default text
        // that says "Unknown Quantity", so the TextView isn't blank.
        if (TextUtils.isEmpty(productQuantity)) {
            productQuantity = context.getString(R.string.unknown_quantity);
        }
        // Update the TextViews with the attributes for the current Product
        nameTextView.setText(productName);
        quantityTextView.setText(productQuantity);
        priceTextView.setText(productPrice);

        final int quantityDb = Integer.parseInt(productQuantity);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantitySale = 0;
                if (quantityDb > 0) {
                    quantitySale = quantityDb - 1;
                }

                ContentValues values = new ContentValues();
                values.put(InventoryEntry.COLUMN_QUANTITY, quantitySale);

                Uri newSaleUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, productId);
                context.getContentResolver().update(newSaleUri, values, null, null);
            }
        });

    }
}
