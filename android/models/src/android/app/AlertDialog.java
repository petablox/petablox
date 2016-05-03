import edu.stanford.stamp.harness.ApplicationDriver;
import edu.stanford.stamp.harness.Callback;

class AlertDialog
{
	class Builder
	{
		public  android.app.AlertDialog.Builder setPositiveButton(int textId, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setPositiveButton(java.lang.CharSequence text, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setNegativeButton(int textId, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setNegativeButton(java.lang.CharSequence text, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setNeutralButton(int textId, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setNeutralButton(java.lang.CharSequence text, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setOnCancelListener(final android.content.DialogInterface.OnCancelListener onCancelListener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							onCancelListener.onCancel(null);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setOnKeyListener(final android.content.DialogInterface.OnKeyListener onKeyListener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							onKeyListener.onKey(null, 0, null);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setItems(int itemsId, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setItems(java.lang.CharSequence[] items, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setAdapter(android.widget.ListAdapter adapter, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setCursor(android.database.Cursor cursor, final android.content.DialogInterface.OnClickListener listener, java.lang.String labelColumn) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setMultiChoiceItems(int itemsId, boolean[] checkedItems, final android.content.DialogInterface.OnMultiChoiceClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0, false);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setMultiChoiceItems(java.lang.CharSequence[] items, boolean[] checkedItems, final android.content.DialogInterface.OnMultiChoiceClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0, false);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setMultiChoiceItems(android.database.Cursor cursor, java.lang.String isCheckedColumn, java.lang.String labelColumn, final android.content.DialogInterface.OnMultiChoiceClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0, false);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setSingleChoiceItems(int itemsId, int checkedItem, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setSingleChoiceItems(android.database.Cursor cursor, int checkedItem, java.lang.String labelColumn, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setSingleChoiceItems(java.lang.CharSequence[] items, int checkedItem, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}

		public  android.app.AlertDialog.Builder setSingleChoiceItems(android.widget.ListAdapter adapter, int checkedItem, final android.content.DialogInterface.OnClickListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onClick(null, 0);
						}
					});
			return this;
		}
		
		public  android.app.AlertDialog.Builder setOnItemSelectedListener(final android.widget.AdapterView.OnItemSelectedListener listener) 
		{ 
			ApplicationDriver.getInstance().
				registerCallback(new Callback(){
						public void run() {
							listener.onItemSelected(null, null, 0, 0L);
							listener.onNothingSelected(null);
						}
					});
			return this;
		}
		
	}
}