package com.example.android.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = EditorActivity.class.getName();
    /**
     * Identifier for the inventory data loader
     */
    private static final int EXISTING_INVENTORY_LOADER = 0;
    private static final int PICK_REQUEST = 0;
    /**
     * EditText field to enter the product's name
     */
    private EditText mNameEditText;
    /**
     * ImageView field for product image
     */
    private ImageView mProductImage;
    /**
     * EditText field to enter the product's quantity
     */
    private EditText mQuantityEditText;
    /**
     * EditText field to enter the product's price
     */
    private EditText mPriceEditText;
    private Button mContactButton;
    private Button mDeleteProduct;
    private Button mDecrementButton;
    private Button mIncrementButton;
    private Button mImageButton;
    private Uri mUri;
    /**
     * Content URI for the existing product (null if it's a new product)
     */
    private Uri mCurrentInventoryUri;

    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not (false)
     */
    private boolean mProductHasChanged = false;
    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mProductHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentInventoryUri = intent.getData();

        if (mCurrentInventoryUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));

            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);

            Log.i(LOG_TAG, "Uri logged...");
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = findViewById(R.id.edit_product_name);
        mProductImage = findViewById(R.id.edit_image);
        mQuantityEditText = findViewById(R.id.edit_product_quantity);
        mPriceEditText = findViewById(R.id.edit_price);
        mImageButton = findViewById(R.id.add_image_button);

        //OnTouchListeners that monitor if a user has touched or modified an input field
        //This allows us to monitor if there are saved/unsaved changes made.
        mNameEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mProductImage.setOnTouchListener(mTouchListener);

        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImagePicker();
            }
        });

        mContactButton = findViewById(R.id.supplier_button);
        mContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String productName = mNameEditText.getText().toString().trim();

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setType("text/plain");
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "New Order: " + productName);
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Product:  "
                        + productName + "\n"
                        + "Order Quantity:  ");

                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(emailIntent);
                }
            }
        });

        mDeleteProduct = findViewById(R.id.editor_delete_product);
        mDeleteProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeleteConfirmationDialog();
            }
        });

        mDecrementButton = findViewById(R.id.decrement_button);
        mDecrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decreaseQuantity(mQuantityEditText);
            }
        });

        mIncrementButton = findViewById(R.id.increment_button);
        mIncrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                increaseQuantity(mQuantityEditText);
            }
        });

    }

    // Starts file picker activity
    // shows us list of available files with  "image" type.
    //MyShareImageExample app by Carlos Jimenez (Udacity mentor) used as reference code.

    public void openImagePicker() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_REQUEST);
    }
    // Receive intent code to open gallery and pick image
    // Location of Image is provided in a Uri, which is then set to the the Product ImageView.
    // MyShareImageExample app by Carlos Jimenez (Udacity mentor) used as reference code.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_REQUEST && resultCode == Activity.RESULT_OK) {

            if (data != null) {
                mUri = data.getData();
                Log.i(LOG_TAG, "Uri: " + mUri.toString());

                mProductImage.setImageBitmap(getBitmapFromUri(mUri));
            }
        }
    }

    // Displays the product image in the ImageView,
    // using an URI for the Image's database location the method returns a bitmap object
    // which is specifically scaled to the {mProductImage} ImageView it is being set to.
    // MyShareImageExample app by Carlos Jimenez (Udacity mentor) used as a reference for the code.
    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get dimensions of the View
        int targetWidth = mProductImage.getWidth();
        int targetHeight = mProductImage.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get bitmap dimensions
            BitmapFactory.Options bitMap = new BitmapFactory.Options();
            bitMap.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bitMap);
            input.close();

            int photoWidth = bitMap.outWidth;
            int photoHeight = bitMap.outHeight;

            // Calculate rescale of image
            int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);

            // Decode image to a Bitmap, scaled to fill the View
            bitMap.inJustDecodeBounds = false;
            bitMap.inSampleSize = scaleFactor;
            bitMap.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitMap);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, "Failed to load product image.", fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load product image.", e);
            return null;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ioe) {
            }
        }
    }

    public void decreaseQuantity(View view) {
        String quantityEt = mQuantityEditText.getText().toString();
        if (TextUtils.isEmpty(quantityEt)) {
            Toast.makeText(this, R.string.quantity_required, Toast.LENGTH_SHORT).show();
        } else {
            int quantityValue = Integer.parseInt(quantityEt);
            int quantityDecrement = quantityValue - 1;

            if (quantityDecrement < 0) {
                return;
            }
            displayDecrease(quantityDecrement);
        }
    }

    private void displayDecrease(int number) {
        mQuantityEditText.setText("" + number);
    }

    public void increaseQuantity(View view) {
        String quantityEt = mQuantityEditText.getText().toString();
        if (TextUtils.isEmpty(quantityEt)) {
            Toast.makeText(this, R.string.quantity_required, Toast.LENGTH_SHORT).show();
        } else {
            int quantityValue = Integer.parseInt(quantityEt);
            int quantityIncrement = quantityValue + 1;
            displayIncrease(quantityIncrement);
        }
    }

    private void displayIncrease(int number) {
        mQuantityEditText.setText("" + number);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentInventoryUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                saveProduct();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                //If product data hasn't changed continue with navigating to parent {@link Main Activity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
                }
                //If there are unsaved changes, prompt a dialog to warn the user
                // Create a click listener to handle the user confirming that changes should be discarded

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //User clicked "Discard" button, navigate to parent activity
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product data hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    /**
     * Get user input from editor and save product into database.
     */
    private void saveProduct() {
        // Read from input fields

        String nameString = mNameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.missing_product_name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (mUri == null) {
            Toast.makeText(this, getString(R.string.missing_image),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String imageString = mUri.toString();

        int quantity;
        String quantityString = mQuantityEditText.getText().toString().trim();
        if (TextUtils.isEmpty(quantityString)) {
            Toast.makeText(this, getString(R.string.missing_quantity),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            quantity = Integer.parseInt(quantityString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_number),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int price;
        String priceString = mPriceEditText.getText().toString().trim();
        if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.missing_price),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            price = Integer.parseInt(priceString);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.invalid_number),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if this is supposed to be a new Product
        // and check if all the fields in the editor are blank
        if (mCurrentInventoryUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(imageString) &&
                TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(priceString)) {
            // Since no fields were modified, we can return early without creating a new inventory.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, "No product added", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(InventoryEntry.COLUMN_IMAGE, imageString);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantity);
        values.put(InventoryEntry.COLUMN_PRICE, price);

        // Determine if this is a new or existing product by checking if mCurrentInventoryUri is null or not
        if (mCurrentInventoryUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentInventoryUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentInventoryUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentInventoryUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_IMAGE,
                InventoryEntry.COLUMN_QUANTITY,
                InventoryEntry.COLUMN_PRICE};
        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentInventoryUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
            int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_IMAGE);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
//            String image = cursor.getString(imageColumnIndex);
//            final Uri imageUri = Uri.parse(image);
            mUri = Uri.parse(cursor.getString(imageColumnIndex));
            int quantity = cursor.getInt(quantityColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mQuantityEditText.setText(String.valueOf(quantity));
            mPriceEditText.setText(String.valueOf(price));

            //Update ImageView with associated image in database
            // MyShareImageExample app by Carlos Jimenez (Udacity mentor) used as a reference for the code.
            ViewTreeObserver viewTreeObserver = mProductImage.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mProductImage.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    mProductImage.setImageBitmap(getBitmapFromUri(mUri));
                }
            });
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mQuantityEditText.setText("");
        mPriceEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        if (mCurrentInventoryUri != null) {
            int rowsDeleted = getContentResolver().delete(
                    mCurrentInventoryUri, //the current product URI
                    null,
                    null);

            // Show a toast message depending on whether or not the insertion was successful.
            if (rowsDeleted == 0) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

    }
}
