package uk.ac.ic.bss.labbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ic.bss.labbook.backend.RegistrationActivity;
import uk.ac.ic.bss.labbook.backend.Util;
import uk.ac.ic.bss.labbook.backend.Util.Barcode;
import uk.ac.ic.bss.labbook.barcode.IntentIntegrator;
import uk.ac.ic.bss.labbook.barcode.IntentResult;
import uk.ac.ic.bss.labbook.calculator.Calculator;
import uk.ac.ic.bss.labbook.media.AudioRecorder;
import uk.ac.ic.bss.labbook.media.ImageViewer;
import uk.ac.ic.bss.labbook.media.VideoPlayer;
import uk.ac.ic.bss.labbook.models.Notebook;
import uk.ac.ic.bss.labbook.models.Notebooks;
import uk.ac.ic.bss.labbook.stocksolution.StockSolutionActivity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.analytics.easytracking.TrackedActivity;

public class LabBookActivity extends TrackedActivity implements OnClickListener {

	private static final int ACTIVITY_CAP_PHOTO=1;
	private static final int ACTIVITY_SELECT_PICTURE=2;
	private static final int ACTIVITY_VIDEO=3;
	private static final int ACTIVITY_SPEECH=4;
	private static final int ACTIVITY_CALCULATOR=5;
	private static final int ACTIVITY_STOCKSOLUTION=6;
	private static final int ACTIVITY_ATTACHMENT = 7;
	private static final int MENU_INC_PEN=12;
	private static final int MENU_DEC_PEN=13;
	private static final int MENU_INC_ERASER=14;
	private static final int MENU_DEC_ERASER=15;
	private static final int MENU_ADD_PAGE=18;
	private static final int MENU_RED_PEN=19;
	private static final int MENU_BLUE_PEN=20;
	private static final int MENU_GREEN_PEN=21;
	private static final int MENU_LOOKUP=22;
	private static final Pattern FILENAME_PATTERN = Pattern.compile("(vid|pic|aud|doc)_\\d+\\.(jpe?g|png|tif|gif|3gp|mp4|pdf|docx?|pptx?|xlsx?)");
	private EditText etext, titletext;
	private TextView pagetext; 
	private CustomScrollView myscroll;
	//private File thisimagefile;
	//private String experiment = "";
	private Notebook notebook;
	private File tmp;
	private InputMethodManager imm;
	//private ViewFlipper flipper;
	//private Vector<EditText>flipper_et = new Vector<EditText>();
	//private Vector<DrawOverlay>flipper_overlay = new Vector<DrawOverlay>();
	//private int flipper_pos = 1; // RESET TO 0 IF MAIN.XML USED FOR OVERLAY 
	private File selected_image, selected_video;
	//private LinkedHashMap<String, String> fileshash = new LinkedHashMap<String, String>();
	private boolean keyboardshowing = false;
	public boolean audioactive = false, audioedit = false;
	private boolean haveaudioid = false;
	private String selected_type;
	private AudioRecorder recorder;
	private File audioid, docid;
	private TextView audiotv;
	private ImageButton recButton, playButton, stopButton;
	private DrawOverlay doverlay;
	private int page_num, max_page;
	private RelativeLayout rlayout;
	private int linenum = 52, lineheight, currentline = 0; 

	private Button penButton;
	private Button eraseButton;
	private Button keyboardButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		System.out.println("@@@onCreate");

		setContentView(R.layout.main);
		super.setTitle("LabBook");

		tmp = new File(getExternalCacheDir(), "tmp");

		// flipper_overlay.addElement((DrawOverlay) findViewById(R.id.overlay));
		rlayout =(RelativeLayout) findViewById(R.id.rlayout1);
		// doverlay = (DrawOverlay) findViewById(R.id.overlay);
		titletext = (EditText) findViewById(R.id.titletext);
		pagetext = (TextView) findViewById(R.id.pagetext);
		// etext = (EditText) findViewById(R.id.notetext);
		myscroll = (CustomScrollView) findViewById(R.id.myscroll);
		// flipper = (ViewFlipper) findViewById(R.id.myflipper);

		// Need to add listener here and not use one in CustomScrollView in order to detect when
		// image has been clicked on in EditText
		myscroll.setOnTouchListener( new MyOnTouchListener());
		// myscroll.setOnLongClickListener(new MyLongClickListener());

		//myscroll.setOnLongClickListener(new MyOnLongClickListener());

		//flipper_et.addElement(etext);
		// flipper_overlay.addElement(doverlay);

		notebook = Notebooks.get((File) getIntent().getExtras().get(NotesList.KEY_FOLDER));
		titletext.setText(notebook.getName());
		//titletext.setEnabled(false);	
		//pagetext.setEnabled(false);

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		etext = new EditText(this);

		etext.setLongClickable(false);
		// This gets fired after 500ms so will triggers while scrolling etc
		//etext.setOnLongClickListener(new MyOnLongClickListener());
		etext.setGravity(Gravity.TOP);
		etext.setGravity(Gravity.LEFT);
		etext.setId(1);
		etext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

		lineheight = etext.getLineHeight();

		etext.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {

				// if enter is pressed start calculating
				/* if (keyCode == KeyEvent.KEYCODE_ENTER
	                    && event.getAction() == KeyEvent.ACTION_UP) {

	                // get EditText text
	                String text = ((EditText) v).getText().toString();

	                // find how many rows it cointains
	                int editTextRowCount = text.split("\\n").length;

	                // user has input more than limited - lets do something
	                // about that
	                // Only add a new page if this is the last one
	                if (editTextRowCount > 50 && page_num == max_page) {

	                    // find the last break
	                    int lastBreakIndex = text.lastIndexOf("\n");

	                    // compose new text
	                    String newText = text.substring(0, lastBreakIndex);

	                    // add new text - delete old one and append new one
	                    // (append because I want the cursor to be at the end)
	                    ((EditText) v).setText("");
	                    ((EditText) v).append(newText);


	                    newText = text.substring(lastBreakIndex, text.length());
	                    saveData(1);
	                    addNewPage(newText+"\n");
	                }
	            } */
				//Log.i("TEXTVIEW SIZE", ""+etext.getLineCount()+" "+etext.getLineHeight());
				if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU
						&& etext.getLineCount() > linenum && etext.getLineCount() != currentline) {
					// doverlay.showalert) {
					imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
					keyboardButton.setSelected(false);
					//if(page_num < max_page)
					//	showAlert("End of Page", "End of page reached");
					//else
					currentline = etext.getLineCount();
					showAddNewPage("End of page reached. Add new page?");
					doverlay.showalert = false;
				}

				return false;
			}
		}); 

		doverlay = new DrawOverlay(this);

		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;
		doverlay.setEditText(width, height*2); // etext, myscroll, 

		doverlay.setBackgroundColor(Color.TRANSPARENT);

		myscroll.setDoverlay(doverlay);

		lp2.addRule(RelativeLayout.ALIGN_TOP, etext.getId()); 
		lp2.addRule(RelativeLayout.ALIGN_BOTTOM, etext.getId());
		//lp2.addRule(RelativeLayout.ALIGN_LEFT, etext.getId());
		//lp2.addRule(RelativeLayout.ALIGN_RIGHT, etext.getId());

		//DisplayMetrics displaymetrics = new DisplayMetrics();
		//getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		//int height = displaymetrics.heightPixels;
		//int wwidth = displaymetrics.widthPixels;

		// etext.setWidth(wwidth);
		// etext.setHeight((wwidth * 14)/10);  // 1.4

		// etext.setMaxHeight((wwidth * 14)/10);
		etext.setMinLines(51);

		rlayout.addView(etext, lp);
		rlayout.addView(doverlay, lp2);

		keyboardButton = (Button) findViewById(R.id.keyboardButton);

		penButton = (Button) findViewById(R.id.penButton);
		registerForContextMenu(penButton);
		penButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (!penButton.isSelected()) {
					onClick(view);
				}
				return false;
			}
		});

		eraseButton = (Button) findViewById(R.id.eraseButton);
		registerForContextMenu(eraseButton);
		eraseButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {
				if (!eraseButton.isSelected()) {
					onClick(view);
				}
				return false;
			}
		});

		registerForContextMenu(findViewById(R.id.cameraButton));
		registerForContextMenu(findViewById(R.id.volumeButton));
		registerForContextMenu(findViewById(R.id.flaskButton));

		max_page = Math.max(1, notebook.txtdir.list().length);
		page_num = LabBook.prefs.getInt(String.format("%s:pageNumber", notebook.id), 1);

		fillPage();

		/*while(f.exists()){
				addFlip();
				file_num++;
				f = new File(NotesList.txtdir + "/" + experiment +"."+ file_num + ".txt");
			}

			for(int i = 1; i <= flipper_et.size(); i++){
				text = getTextFromFile(i);
				flipper_et.elementAt(i-1).setText(Html.fromHtml(text));
				flipper_et.elementAt(i-1).setSelection(0);

				addAllImages(flipper_et.elementAt(i-1).getText().toString(), i-1);
				//addAllImages(text, i-1);

				f = new File(NotesList.bmpdir + "/" + experiment +"."+ i + ".png"); // book_version
				if(f.exists()){
					//Log.i("ADDING:", NotesList.bmpdir + "/" + experiment +"."+ i + ".png");
					Bitmap bmp = BitmapFactory.decodeFile(NotesList.bmpdir + "/" + experiment +"."+ i + ".png");
					flipper_overlay.elementAt(i-1).setBitmap(bmp); //BitmapFactory.decodeFile(NotesList.bmpdir + "/" + experiment +"."+ i + ".png"));
					try{
						bmp.recycle();
					}
					catch(NullPointerException npe){}
				}
			}*/
		//}

		// Prevent keyboard appearing when app starts
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		//etext = flipper_et.elementAt(0);

		//		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		//imm.showSoftInput(etext, 0);

		imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);

		/*etext.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(!keyboardshowing){
					imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
				}

			}
			});*/

		//etext.setInputType(InputType.TYPE_NULL);
		//imm.showSoftInput(etext, 0);

		PackageManager pm = getPackageManager();
		if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			findViewById(R.id.cameraButton).setEnabled(false);
			findViewById(R.id.scanButton).setEnabled(false);
		}
	}

	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.prevbut:
			if (page_num > 1) {
				saveData();
				flipPrevious();
			}
			break;
		case R.id.keyboardButton:
			if (keyboardButton.isSelected()) {
				hideKeyboard();
			} else {
				penButton.setSelected(false);
				doverlay.candraw = false;
				eraseButton.setSelected(false);
				keyboardshowing = true;
				imm.showSoftInput(etext, 0);
				keyboardButton.setSelected(true);
			}
			break;
		case R.id.penButton:
			if (penButton.isSelected())	{
				penButton.setSelected(false);
				doverlay.candraw = false;
			} else {
				penButton.setSelected(true);
				doverlay.candraw = true;
				doverlay.setDrawing();
				eraseButton.setSelected(false);
				if (keyboardshowing) {
					hideKeyboard();
				}
			}
			break;
		case R.id.eraseButton:
			if (eraseButton.isSelected()) {
				eraseButton.setSelected(false);
				doverlay.candraw = false;
			} else {
				eraseButton.setSelected(true);
				doverlay.candraw = true;
				doverlay.erase();
				penButton.setSelected(false);
				if (keyboardshowing) {
					hideKeyboard();
				}
			}
			break;
		case R.id.cameraButton:
		case R.id.volumeButton:
		case R.id.flaskButton:
			view.performLongClick();
			break;
		case R.id.scanButton:
			if (!doverlay.canExpand()) {
				showAddNewPage("End of page reached. Add new page?");
			} else {
				IntentIntegrator.initiateScan(LabBookActivity.this);
				if (keyboardshowing) {
					hideKeyboard();
				}
			}
			break;
		case R.id.nextbut:
			if (new File(notebook.txtdir, (page_num + 1) + ".txt").exists()) {
				saveData();
				flipNext();
			} else {
				showAddNewPage("Add a new page?");
			}
			break;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();

		menu.add(0, MENU_INC_PEN, 0, "Pen Size +");
		menu.add(0, MENU_DEC_PEN, 0, "Pen Size -");
		menu.add(0, MENU_INC_ERASER, 0, "Eraser Size +");
		menu.add(0, MENU_DEC_ERASER, 0, "Eraser Size -");
		menu.add(0, MENU_ADD_PAGE, 0, "Add Page");

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);

		switch(item.getItemId()) {
		case MENU_INC_PEN:
			NotesList.pensize++;
			//for(DrawOverlay dover : flipper_overlay)
			doverlay.setPenSize();
			break;
		case MENU_DEC_PEN:
			NotesList.pensize--;
			//for(DrawOverlay dover : flipper_overlay)
			doverlay.setPenSize();
			break;
		case MENU_INC_ERASER:
			NotesList.erasersize++;
			//for(DrawOverlay dover : flipper_overlay)
			doverlay.setEraserSize();
			break;
		case MENU_DEC_ERASER:
			NotesList.erasersize--;
			//for(DrawOverlay dover : flipper_overlay)
			doverlay.setEraserSize();
			break;
		case MENU_ADD_PAGE:
			saveData();
			page_num = max_page;
			addNewPage();
			break;
		}
		return true;
	}

	private void addImage(File file) {
		addImage(file, etext.getSelectionStart(), etext.getSelectionEnd(), true);
	}

	private void addImage(File file, int start, int end, boolean newline) {
		Drawable drawable;
		if (notebook.audioMemo.equals(file.getParentFile())) {
			drawable = getResources().getDrawable(R.drawable.audio);
		} else if (notebook.docs.equals(file.getParentFile())) {
			drawable = getResources().getDrawable(attachmentTypes.get(Util.getMimeType(file)));
		} else {
			drawable = new BitmapDrawable(getResources(), BitmapFactory.decodeFile(file.getPath()));
		}
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

		SpannableString spannableString = new SpannableString(file.getName());
		spannableString.setSpan(new ImageSpan(drawable), 0, file.getName().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		etext.getText().replace(start, end, spannableString);

		linenum -= (drawable.getIntrinsicHeight() / lineheight - 1);
	}

	private void addAllImages(String text) {
		Matcher m = FILENAME_PATTERN.matcher(text);
		while (m.find()) {
			String fileName = m.group();
			File folder;
			if (fileName.startsWith("aud_")) {
				folder = notebook.audioMemo;
			} else if (fileName.startsWith("doc_")) {
				folder = notebook.docs;
			} else {
				folder = notebook.thumbs;
			}
			addImage(new File(folder, fileName), m.start(), m.end(), false);
		}
	}

	private String getTextFromFile(int i) {
		return Util.read(new File(notebook.txtdir, i + ".txt"));
	}

	public void save(View view) {
		saveData();
	}

	public void addAttachment(View view) {
		if (!doverlay.canExpand()) {
			showAddNewPage("End of page reached. Add new page?");
		} else {
			Intent intent = new Intent();
			intent.setType("application/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, "Choose file"), ACTIVITY_ATTACHMENT);
		}
	}

	@SuppressWarnings("serial")
	private static Map<String, Integer> attachmentTypes = new HashMap<String, Integer>() {{
		put("application/pdf", R.drawable.pdf);
		put("application/msword", R.drawable.document);
		put("application/vnd.ms-excel", R.drawable.spreadsheet);
		put("application/vnd.ms-powerpoint", R.drawable.presentation);
		put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", R.drawable.document);
		put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", R.drawable.spreadsheet);
		put("application/vnd.openxmlformats-officedocument.presentationml.presentation", R.drawable.presentation);
	}};
	private void addAttachment(Uri uri) {
		//hack for OI File Manager
		String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString().replace(" ", "%20"));
		String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		if (attachmentTypes.containsKey(type)) {
			File out = new File(notebook.docs, String.format("doc_%s.%s", System.currentTimeMillis(), extension));
			try {
				Util.copy(getContentResolver().openInputStream(uri), new FileOutputStream(out));
				addImage(out);
				//hack for Documents To Go
				if (!Uri.decode(uri.getLastPathSegment()).equals(uri.getLastPathSegment())) {
					uri = Uri.parse(Uri.decode(uri.getLastPathSegment()));
				}
				etext.append(String.format("%s%n%n", uri.getLastPathSegment()));
			} catch (IOException e) {
				Toast.makeText(this, "Failed to copy file", Toast.LENGTH_LONG).show();
				throw new RuntimeException(e);
			}
		} else {
			Toast.makeText(this, "Selected file is not an Office document", Toast.LENGTH_LONG).show();
		}
	}

	private void saveData() {
		System.out.println("@@@saveData");

		if (!modified()) {
			return;
		}

		if (nameModified()) {
			String set_experiment = titletext.getText().toString();
			if (set_experiment.length() == 0) {
				titletext.setText(notebook.getName());
			} else if (!set_experiment.equals(notebook.getName())) {
				notebook.setName(set_experiment);
			}
		}

		if (overlayModified() || textModified()) {
			//Log.i("COUNT", i+" "+flipper.getChildCount()+" "+flipper.getChildAt(i-1));
			//view = (RelativeLayout)flipper.getChildAt(i-1);
			RelativeLayout view = rlayout; 

			// If the view contains text and/or an overlay image then save it
			if(view.getWidth() > 0 && view.getHeight() > 0){
				Bitmap bm = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

				//flipper_et.elementAt(i-1).setCursorVisible(false);
				//etext.setCursorVisible(false);
				etext.clearFocus();
				view.draw(new Canvas(bm));

				view.setDrawingCacheEnabled(true);
				//	bm = view.getDrawingCache();
				//bm.compress(CompressFormat.JPEG, 95, new FileOutputStream
				//flipper_et.elementAt(i-1).setCursorVisible(true);

				try {
					FileOutputStream out = new FileOutputStream(new File(notebook.screenshots, page_num + ".png")); // book_version
					bm.compress(Bitmap.CompressFormat.PNG, 100, out);
					out.close();
				} catch (Exception e) {
					e.printStackTrace();
					Log.i("FILE BMP WRITE ERROR", e.toString());
				}
				thumbnailNote();

				//etext.setCursorVisible(true);
				etext.requestFocus();
				bm.recycle();
				bm = null;
			} 
			System.gc();
		}

		if (textModified()) {
			try {
				Writer writer = new FileWriter(new File(notebook.txtdir, page_num + ".txt"));
				writer.write(etext.getText().toString());
				writer.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (overlayModified()) {
			doverlay.saveBitmap(new File(notebook.bmpdir, page_num + ".png").getPath());
			//overlaybmp.recycle();
		}

		notebook.setLastModified();

		Util.sync(this);
	}


	public Bitmap combineImages(Bitmap image1, Bitmap image2) {
		// Bitmap[] mBitmap = new Bitmap[6];

		int width = 0, height = 0;
		if (image1.getWidth() > image2.getWidth()) {
			width = image1.getWidth();
			height = image1.getHeight() + image2.getHeight();
		} else {
			width = image2.getWidth();
			height = image1.getHeight() + image2.getHeight();
		}
		Bitmap combinedImages = null;
		combinedImages = Bitmap.createBitmap(width * 2, height, Bitmap.Config.ARGB_8888);
		Canvas comboImage = new Canvas(combinedImages);
		comboImage.drawBitmap(image1, 0f, 0f, null);
		comboImage.drawBitmap(image2, 0f, image1.getHeight()+1, null);
		return combinedImages;
	}

	private static List<String> imageTypes = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/tiff");
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode != RESULT_OK) {
			return;
		}

		switch (requestCode) {
		case ACTIVITY_CAP_PHOTO:
			File pic = new File(notebook.picdir, String.format("pic_%s.jpg", System.currentTimeMillis()));
			try {
				Util.copy(new FileInputStream(tmp), new FileOutputStream(pic));
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
			addImage(thumbnailPicdir(pic));
			etext.append("\n\n");
			break;
		case ACTIVITY_SELECT_PICTURE:
			Uri selectedImageUri = intent.getData();

			//OI FILE Manager
			String filemanagerstring = selectedImageUri.getPath();

			//MEDIA GALLERY
			String selectedImagePath = getPath(selectedImageUri);

			String fpath = selectedImagePath != null ? selectedImagePath : filemanagerstring;
			if (fpath.length() > 0){
				final File in = new File(fpath);
				String mimeType = Util.getMimeType(in);
				if (imageTypes.contains(mimeType)) {
					final File out = new File(notebook.picdir, String.format("pic_%s.%s", System.currentTimeMillis(), Util.getExtension(in)));
					try {
						Util.copy(new FileInputStream(in), new FileOutputStream(out));
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
					if ("image/tiff".equals(mimeType)) {
						if (RegistrationActivity.isRegistered()) {
							final Uri serverUri = Uri.parse(Util.getProperty(getResources(), R.raw.server, "external.url"));
							new AsyncTask<Void, Void, Void>() {
								private ProgressDialog dialog;
								File src;
								@Override
								protected void onPreExecute() {
									dialog = ProgressDialog.show(LabBookActivity.this, "Converting...", "Image conversion in progress", true, false);
								};
								@Override
								protected Void doInBackground(Void... params) {
									try {
										src = new File(getExternalCacheDir(), out.getName());
										Util.convert(serverUri, in, src);
									} catch (IOException e) {
										e.printStackTrace();
									}
									return null;
								}
								protected void onPostExecute(Void result) {
									dialog.dismiss();
									addImage(thumbnailPicdir(src));
									etext.append("\n\n");
								};
							}.execute();
						}
					} else {
						addImage(thumbnailPicdir(out));
						etext.append("\n\n");
					}
				}
			}
			break;
		case ACTIVITY_VIDEO:
			importVideo(intent.getData());
			break;
		case ACTIVITY_SPEECH:
			if(intent.getExtras() != null){
				int start = etext.getSelectionStart();
				int end = etext.getSelectionEnd();
				etext.getText().replace(Math.min(start, end), Math.max(start, end), intent.getStringExtra("SPEECH"));
			}
			break;
		case ACTIVITY_CALCULATOR:
			Log.i("ACTIVITY_CALCULATOR", "1");
			etext.append(intent.getStringExtra("returnKey1"));
			break;
		case ACTIVITY_STOCKSOLUTION:
			Log.i("ACTIVITY_STOCKSOLUTION", "1");
			etext.append(intent.getStringExtra("returnKey1"));
			break;
		case IntentIntegrator.REQUEST_CODE:
			if (resultCode != RESULT_CANCELED) {
				IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
				if (scanResult != null) {
					lookupBarcode(scanResult.getContents());
				}
			}
			break;
		case ACTIVITY_ATTACHMENT:
			if (resultCode == RESULT_OK) {
				addAttachment(intent.getData());
			}
			break;
		}
	}

	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		if(cursor!=null)
		{
			//HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
			//THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		}
		else return null;
	}

	private File thumbnailPicdir(File file) {
		File thumb = new File(notebook.thumbs, file.getName());

		int torotate = 0;
		try {
			switch (new ExifInterface(file.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				torotate = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				torotate = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				torotate = 270;
				break;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		//measure
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(file.getPath(), options);
		//System.out.printf("@@@%s %s %s%n", options.outWidth, options.outHeight, torotate);

		//resize
		int width = torotate == 0 || torotate == 180 ? options.outWidth : options.outHeight;
		int reqWidth = getWindowManager().getDefaultDisplay().getWidth();
		options.inSampleSize = (int) FloatMath.ceil((float) width / reqWidth);
		options.inJustDecodeBounds = false;
		Bitmap thumbnail = BitmapFactory.decodeFile(file.getPath(), options);
		//System.out.printf("@@@%s %s %s%n", reqWidth, options.inSampleSize, thumbnail.getWidth());

		//rotate
		Matrix matrix = new Matrix();
		width = torotate == 0 || torotate == 180 ? thumbnail.getWidth() : thumbnail.getHeight();
		float scale = 0.8f * getWindowManager().getDefaultDisplay().getWidth() / width;
		matrix.setScale(scale, scale);
		matrix.postRotate(torotate);
		thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
		//System.out.printf("@@@%s %s %s%n", width, scale, thumbnail.getWidth());

		//save
		try {
			thumbnail.compress(Bitmap.CompressFormat.JPEG, 50, new FileOutputStream(thumb)) ;
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}

		return thumb;
	}

	private File thumbnailVideos(File file) throws IOException {
		Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(file.getAbsolutePath(), MediaStore.Video.Thumbnails.MICRO_KIND);
		File thumbnail = new File(notebook.thumbs, file.getName());
		OutputStream out = new FileOutputStream(thumbnail);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
		out.close();
		return thumbnail;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		System.out.println("@@@onDestroy");

		doverlay.mBitmap = null;
		doverlay = null;
		etext = null;
		myscroll = null;
		rlayout = null;
		System.gc();
	}

	@Override
	protected void onPause() {
		super.onPause();

		System.out.println("@@@onPause");

		saveData();
	}

	@Override
	protected void onStop() {
		super.onStop();

		System.out.println("@@@onStop");

		LabBook.prefs.edit()
		.putInt(String.format("%s:pageNumber", notebook.id), page_num)
		.putInt(String.format("%s:%s:cursorPosition", notebook.id, page_num), etext.getSelectionStart())
		.commit();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);

		System.out.println("@@@onSaveInstanceState");

		//		//save: page, thisimagefile, cursorpos, canexpand, showlaert
		//
		//		//outState.putInt("page", flipper_pos);
		//		//outState.putInt("cursorpos", flipper_et.get(flipper_pos-1).getSelectionStart());
		//		outState.putInt("page", page_num);
		//		if (thisimagefile != null) {
		//			outState.putString("thisimagefile", thisimagefile.getName());
		//		}
		//
		//		outState.putBoolean("canexpand", doverlay.canexpand);
		//		outState.putBoolean("showlaert", doverlay.showalert);
		//
		//		saveData();
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		System.out.println("@@@onRestoreInstanceState");

		//restore: page, thisimagefile, cursorpos, canexpand, showlaert

		//		page_num = savedInstanceState.getInt("page");
		//		if (savedInstanceState.getString("thisimagefile") != null) {
		//			thisimagefile = new File(notebook.picdir, savedInstanceState.getString("thisimagefile"));
		//		}
		//
		//		fillPage();
		//
		//		doverlay.showalert = savedInstanceState.getBoolean("showalert");
		//		doverlay.canexpand = savedInstanceState.getBoolean("canexpand");
		//
		//		DisplayMetrics displaymetrics = new DisplayMetrics();
		//		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		//		int height = displaymetrics.heightPixels;
		//		int width = displaymetrics.widthPixels;
		//		doverlay.setEditText(width, height*2); // etext, myscroll, 
		//
		//		etext.setSelection(savedInstanceState.getInt("cursorpos"));
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// If the keyboard is showing when back button pressed it is removed
			// so need to reset boolean
			keyboardshowing = false;

			setResult(RESULT_OK);
			finish();
		}
		return false;
	}

	private void flipNext() {
		//boolean candraw = flipper_overlay.elementAt(flipper_pos-1).candraw;

		page_num++;
		fillPage();
	}

	/*flipper.setInAnimation(animateInFrom(RIGHT));
    flipper.setOutAnimation(animateOutTo(LEFT));
	if(flipper_pos == flipper_et.size()){
		showAddFlipAlert(); 

	}
	else{
		flipper.showNext();
		flipper_pos++;
		if(flipper_pos > flipper_et.size())
			flipper_pos = 1;

    	//imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
		etext = flipper_et.elementAt(flipper_pos-1);

		// Flipping will remove keyboard if it is showing
		if(keyboardshowing)
			keyboardshowing = false;
		//else
			//etext.setInputType(InputType.TYPE_NULL); // disable soft input 
		//	imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);

		setFlipValues(candraw);
	}*/

	private void flipPrevious() {
		//boolean candraw = flipper_overlay.elementAt(flipper_pos-1).candraw;

		page_num--;
		fillPage();

		/*flipper.setInAnimation(animateInFrom(LEFT));
    flipper.setOutAnimation(animateOutTo(RIGHT));
	flipper.showPrevious();
	flipper_pos--;
	if(flipper_pos < 1)
		flipper_pos = flipper_et.size();*/

		//imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
		//etext = flipper_et.elementAt(flipper_pos-1);
		//if(keyboardshowing)
		//	keyboardshowing = false;
		//else
		//etext.setInputType(InputType.TYPE_NULL); // disable soft input 
		//	imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);

		//setFlipValues(candraw);
	}

	public void showAddNewPage(String message) {
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				saveData();
				addNewPage();
				/*page_num++;
        	max_page = page_num;
        	String text = getTextFromFile(page_num);
    		etext.setText(Html.fromHtml(text));
    		etext.setSelection(0);
    		addAllImages(text, page_num);
    		//addAllImages(text, i-1);

    		File f = new File(NotesList.bmpdir + "/" + experiment +"."+ page_num + ".png"); 
    		if(f.exists()){
    			//Bitmap bmp = BitmapFactory.decodeFile(NotesList.bmpdir + "/" + experiment +"."+ page_num + ".png");
    			doverlay.setBitmap(NotesList.bmpdir + "/" + experiment +"."+ page_num + ".png"); 
    			try{
    				//bmp.recycle();
    			}
    			catch(NullPointerException npe){}
    		}

    		pagetext.setText("P:"+page_num+"/"+max_page);*/
				/*addFlip();
        	flipper.showNext();
        	flipper_pos++;
        	if(flipper_pos > flipper_et.size())
        		flipper_pos = 1;

        	etext = flipper_et.elementAt(flipper_pos-1);
        	if(keyboardshowing)
        		keyboardshowing = false;

        	setFlipValues(flipper_overlay.elementAt(flipper_pos-1).candraw);*/
			}
		})
		.setNegativeButton("No", null)
		.show();
	}

	private void addNewPage() {
		max_page++;
		flipNext();
	}

	/*private void setFlipValues(boolean candraw){

	myscroll.setEditText(etext);
	etext.requestFocus();
	pagetext.setText("P:"+flipper_pos+"/"+flipper_et.size());
	// If annotation is set switch to text input
	if(candraw){
		//imm.showSoftInput(etext, 0);
	 	for(DrawOverlay dover : flipper_overlay)
	 		dover.candraw = false;
		myscroll.setIsScrollable(true);
	 	mScrollable = true;		
	}
} */

	/*private void addFlip(){ 

	RelativeLayout rl = new RelativeLayout(this);
    rl.setLayoutParams( new ViewGroup.LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT ) );

    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

    flipper.addView(rl); 

    EditText et = new EditText(this);
    et.setMinLines(30);
    et.setLongClickable(false);
    et.setGravity(Gravity.TOP);
    et.setGravity(Gravity.LEFT);
    et.setId(1);
	rl.addView(et, lp2);

	DrawOverlay dover = new DrawOverlay(this);

	dover.setBackgroundColor(Color.TRANSPARENT);

	lp.addRule(RelativeLayout.ALIGN_TOP, et.getId()); 
	lp.addRule(RelativeLayout.ALIGN_BOTTOM, et.getId());

	rl.addView(dover, lp);

	flipper_et.addElement(et);

	//File f = new File(NotesList.bmpdir + "/" + experiment +"."+ flipper_pos + ".png"); 
	//if(f.exists()){
	//	overlaybmp = BitmapFactory.decodeFile(NotesList.bmpdir + "/" + experiment +"."+ flipper_pos + ".png"); 
	//	dover.setBitmap(overlaybmp);
	//	overlaybmp.recycle();
	//}

    flipper_overlay.addElement(dover);

    // If this is the first screen the buttons haven't been initialised
    if(flipper_overlay.size() > 1){
    	try{
    		eraseButton.setSelected(false);
    		penButton.setSelected(false);
    	}
    	catch(NullPointerException npe){

    	}
    }
	for(DrawOverlay dover2 : flipper_overlay)
		dover2.candraw = false;

	//if(keyboardshowing){
	//	keyboardshowing = false;
	//	imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
	//	this.keyboardButton.setSelected(false);
	//	}

} */

	/*private Animation animateInFrom(int fromDirection) {

    Animation inFrom = null;

    switch (fromDirection) {
    case LEFT:
            inFrom = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, -1.0f, 
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f);
            break;
    case RIGHT:
            inFrom = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, +1.0f, 
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f);
            break;
    }

    inFrom.setDuration(250);
    inFrom.setInterpolator(new AccelerateInterpolator());
    return inFrom;
}

private Animation animateOutTo(int toDirection) {

    Animation outTo = null;

    switch (toDirection) {
    case LEFT:
            outTo = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, -1.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f);
            break;
    case RIGHT:
            outTo = new TranslateAnimation(
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, +1.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f,
                            Animation.RELATIVE_TO_PARENT, 0.0f);
            break;
    }

    outTo.setDuration(250);
    outTo.setInterpolator(new AccelerateInterpolator());
    return outTo;
} */

	private void importVideo(Uri uri) {
		try {
			File video = new File(notebook.videos, String.format("vid_%s.mp4", System.currentTimeMillis()));
			Util.copy(getContentResolver().openInputStream(uri), new FileOutputStream(video));
			addImage(thumbnailVideos(video));
			etext.append("\n\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void checkImageSelect(int pos, boolean show){

		Matcher m = Pattern.compile("^" + FILENAME_PATTERN.pattern(), Pattern.DOTALL).matcher(etext.getText().toString().substring(pos));
		if (m.find()) {
			m.reset();
		} else {
			m = Pattern.compile(FILENAME_PATTERN.pattern() + "$", Pattern.DOTALL).matcher(etext.getText().toString().substring(0, pos));
		}

		//		String pre="X", post="Y";
		//		try{
		//			pre = text.substring(pos - 21, pos);
		//		}
		//		catch(IndexOutOfBoundsException obe){
		//			try{
		//				pre = text.substring(0, 21);
		//			}
		//			catch(IndexOutOfBoundsException obe2){}
		//		}
		//		try{
		//			post = text.substring(pos, pos+21);
		//		}
		//		catch(IndexOutOfBoundsException obe){
		//			try{
		//				post = text.substring(pos, text.length() -1);
		//			}
		//			catch(IndexOutOfBoundsException obe2){}
		//		}
		//
		//		Matcher m = FILENAME_PATTERN.matcher(pre + post);
		if (m.find()) {
			String filename = m.group();
			String type = m.group(1);
			String extension = m.group(2);
			if ("pic".equals(type)) {
				selected_image = new File(notebook.picdir, filename);
				selected_type = "Image";
				if(show){

					if (selected_image.exists()) {
						showPicture();
					}
				}
				else
					showDeleteFileAlert("image");
			} else if ("vid".equals(type)) {
				selected_video = new File(notebook.videos, filename.replaceAll("jpg", "mp4"));
				selected_type = "Video";
				if(show){

					if (selected_video.exists()) {
						showVideoAlert();
					}
				}
				else
					showDeleteFileAlert("video");
			}
			else if ("aud".equals(type)) {
				audioid = new File(notebook.audioMemo, filename);
				selected_type = "Audio";
				if(show){

					if (audioid.exists()) {
						showMemoAlert();
					}
				}
				else
					showDeleteFileAlert("audio memo");
			} else if ("doc".equals(type)) {
				docid = new File(notebook.docs, filename);
				selected_type = "Document";
				if(show){
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setDataAndType(Uri.fromFile(new File(notebook.docs, filename)), MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension));
					if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
						startActivity(Intent.createChooser(intent, "Open file"));
					} else {
						new AlertDialog.Builder(this)
						.setMessage("No apps installed to open Office documents. Download one now?")
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.mobisystems.office")));
							}
						})
						.setNegativeButton("No", null)
						.show();
					}
				}
				else
					showDeleteFileAlert("document");
			}
		}
	}

	public void deleteFile(){
		//File f = null;
		String filename = "";
		try{
			if(selected_type.equalsIgnoreCase("Image")){
				selected_image.delete();
				filename = selected_image.getName();
			}
			else if(selected_type.equalsIgnoreCase("Video")){
				selected_video.delete();
				filename = selected_video.getName();
				filename.replaceAll("mp4", "jpg");
			}
			else if(selected_type.equalsIgnoreCase("audioMemo")){
				audioid.delete();
				filename = audioid.getName();
			}
			else if(selected_type.equalsIgnoreCase("Document")){
				docid.delete();
				filename = docid.getName();
			}
		}
		catch(NullPointerException npe){}
		String text;
		Pattern pattern = Pattern.compile("\\s*"+filename+"\\s*");
		Matcher m = pattern.matcher(etext.getText().toString());
		if (m.find()) {
			// The "replace" only removes the text but leaves the spannable image in place.
			// To remove the image the text has to be added to the EditText again 
			etext.getText().replace(m.start(), m.end(), " ");
			text = etext.getText().toString();
			etext.setText(text);
			addAllImages(text);
		}


	}

	private void showPicture(){
		try{
			Intent i = new Intent(this, ImageViewer.class);

			i.putExtra("IMAGE_ID", selected_image);

			startActivity(i);
		}
		catch(Exception e){}
	}

	private void showVideo(){
		Intent i = new Intent(this, VideoPlayer.class);

		i.putExtra("VIDEO_ID", selected_video);
		i.putExtras(this.getIntent().getExtras());

		startActivity(i);
	}

	public void showPictureAlert(){
		new AlertDialog.Builder(this)
		.setTitle("Picture")
		.setMessage("Show picture?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				showPicture();
			}
		})
		.setNegativeButton("No", null)
		.show();	
	}

	public void showVideoAlert(){
		new AlertDialog.Builder(this)
		.setTitle("Video")
		.setMessage("Play video?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				showVideo();
			}
		})
		.setNegativeButton("No", null)
		.show();
	}

	public void showMemoAlert(){
		new AlertDialog.Builder(this)
		.setTitle("Audio Memo")
		.setMessage("Play/Record Memo")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				showAudioInterface();
			}
		})
		.setNegativeButton("No", null)
		.show();	
	}

	public void showDeleteFileAlert(String type){
		new AlertDialog.Builder(this)
		.setTitle("Delete "+type+"?")
		.setMessage("Confirm to permanently delete the "+type)
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				deleteFile();
			}
		})
		.setNegativeButton("No", null)
		.show();	
	}

	//boolean ismoving = false;
	float touch_pos = 0;
	long mseconds = 0;
	long oldmseconds = -10000;

	// We can be in one of these 2 states
	static final int NONE = 0;
	static final int ZOOM = 1;
	int mode = NONE;

	static final int MIN_FONT_SIZE = 10;
	static final int MAX_FONT_SIZE = 50;

	float oldDist = 1f;

	// Could use OnLongTouchListener but also need OnTouchListener so use timer count
	class MyOnTouchListener implements View.OnTouchListener {

		public boolean onTouch(View v, MotionEvent event) {
			float x, x2;

			if (!doverlay.candraw) {
				MotionEvent ev2; // = ev;
				Calendar cal = Calendar.getInstance(); 

				switch (event.getAction() & MotionEvent.ACTION_MASK) {

				case MotionEvent.ACTION_POINTER_DOWN:
					oldDist = spacing(event);
					//Log.d(TAG, "oldDist=" + oldDist);
					if (oldDist > 10f) {
						mode = ZOOM;
						Log.i("ZOOM", "mode=ZOOM" );
					}
					break;

				case MotionEvent.ACTION_POINTER_UP:
					mode = NONE;
					break;

				case MotionEvent.ACTION_MOVE:
					// finger moves on the screen
					//ismoving = true;
					//Log.i("MOVING", "IS MOVING");
					if (mode == ZOOM) {
						float newDist = spacing(event);
						// If you want to tweak font scaling, this is the place to go.
						if (newDist > 10f) {
							float scale = newDist / oldDist;

							if (scale > 1) {
								scale = 1.1f;
							} else if (scale < 1) {
								scale = 0.95f;
							}

							/*  float currentSize = etext.getTextSize() * scale;
	                            if ((currentSize < MAX_FONT_SIZE && currentSize > MIN_FONT_SIZE)
	                                    ||(currentSize >= MAX_FONT_SIZE && scale < 1)
	                                    || (currentSize <= MIN_FONT_SIZE && scale > 1)) {
	                                etext.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentSize);
	                            } */
						}
					}
					break;

				case MotionEvent.ACTION_DOWN: 
					// finger touches the screen
					ev2 = event;
					ev2.setLocation(event.getX(), event.getY()); 
					x = event.getX() - touch_pos;
					x2 = touch_pos - event.getX();
					if((x >= 0 && x < 10) || (x2 >= 0 && x2 < 10)){
						//checkImageSelect(etext.getSelectionStart());
						//imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
						//if(!keyboardshowing){
						//	etext.setInputType(InputType.TYPE_NULL); // disable soft input
						//}
						//if(!keyboardshowing){
						//	imm.hideSoftInputFromWindow(etext.getWindowToken(), WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
						//}

						//etext.performClick(); //onTouchEvent(ev2);
						//etext.setInputType(InputType.TYPE_NULL);
						//imm.hideSoftInputFromWindow(etext.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); 
						etext.onTouchEvent(ev2);
						if(!keyboardshowing){
							imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
						}
						//etext.setInputType(InputType.TYPE_CLASS_TEXT); // restore input type
					}
					touch_pos = event.getX();
					oldmseconds = mseconds;
					mseconds = cal.getTimeInMillis();
					etext.onTouchEvent(ev2);
					if(mseconds - oldmseconds < 500)
						checkImageSelect(etext.getSelectionStart(), true);
					break;

				case MotionEvent.ACTION_UP:   
					// finger leaves the screen
					ev2 = event;
					ev2.setLocation(event.getX(), event.getY() + myscroll.getScrollY());
					//etext.onTouchEvent(event);
					x = event.getX() - touch_pos;
					x2 = touch_pos - event.getX();
					if((x >= 0 && x < 10) || (x2 >= 0 && x2 < 10)){

						//imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
						//if(!keyboardshowing){
						//	imm.hideSoftInputFromWindow(etext.getWindowToken(), WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
						//}
						//etext.performClick(); //onTouchEvent(event);
						//etext.setInputType(InputType.TYPE_NULL);
						//imm.hideSoftInputFromWindow(etext.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); 
						etext.onTouchEvent(ev2);
						if(!keyboardshowing){
							imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
						}
						//etext.setInputType(InputType.TYPE_CLASS_TEXT); // restore input type

						if(cal.getTimeInMillis() - mseconds > 500) {
							if (etext.getSelectionStart() == -1) {
								Log.i("LabBook", "Image selected but no text selected");
							} else {
								checkImageSelect(etext.getSelectionStart(), false);
							}
						}

						//Log.i("DISTANCE", ""+x+" "+x2);
					}
					//ismoving = false;
					break;
				}
			}

			return false;
		}
	}

	/*	class MyOnLongClickListener implements android.view.View.OnLongClickListener{

		@Override
		public boolean onLongClick(View v) {
			Log.i("LONG CLICK", "HERE");
			// TODO Auto-generated method stub
			checkImageSelect(etext.getSelectionStart());
			return false;
		}

	} */

	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}

	/*class MyLongClickListener implements android.view.View.OnLongClickListener{
		public boolean onLongClick(View v) {
			Log.i("LONG CLICK", ""+etext.getSelectionStart());

			checkImageSelect(etext.getSelectionStart());
			return false;
		}
	}*/

	private void showAudioInterface(){

		if(recorder == null)
			recorder = new AudioRecorder();

		TableLayout.LayoutParams l = new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		TableLayout tl=new TableLayout(this);
		//RelativeLayout ll = new RelativeLayout(this);
		//ll.setLayoutParams( new ViewGroup.LayoutParams( LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT ) );

		//RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		recButton = new ImageButton(this);
		//recButton.setText("Rec");
		recButton.setImageResource(R.drawable.record24);
		tl.addView(recButton, l);

		playButton = new ImageButton(this);
		//playButton.setText("Play");
		playButton.setImageResource(R.drawable.play24);
		tl.addView(playButton, l);

		stopButton = new ImageButton(this);
		//stopButton.setText("Stop");
		stopButton.setImageResource(R.drawable.stop24);
		tl.addView(stopButton, l);
		stopButton.setEnabled(false);

		audiotv = new TextView(this);
		audiotv.setGravity(Gravity.CENTER_HORIZONTAL); 
		audiotv.setTextSize(18);
		tl.addView(audiotv, l);

		if(audioid == null){
			playButton.setEnabled(false);
			haveaudioid = false;
			audiotv.setText("No Audio Recorded");
		}
		else{
			haveaudioid = true;
			audiotv.setText("Audio Available");
		}

		recButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				if(audioid != null){
					checkRecordAudio();
				}
				else
					recAudio();
			}

		}); 

		playButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {

				playAudio();
			}

		}); 

		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {

				stopAudio();
			}

		});

		AlertDialog.Builder alert = new AlertDialog.Builder(this);  

		alert.setTitle("Audio Memo");  
		alert.setView(tl);    

		alert.setPositiveButton("Done", new DialogInterface.OnClickListener() { // OK  
			public void onClick(DialogInterface dialog, int whichButton) {
				if(audioactive)
					stopAudio();
				audioid = null;
				audioedit = false;
				return;		  

			}
		});  

		alert.show();    
	}

	public void checkRecordAudio() {
		new AlertDialog.Builder(this)
		.setTitle("Recording Exists")
		.setMessage("Recording Exists - Overwrite?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				recAudio();
			}
		})
		.setNegativeButton("No", null)
		.show();
	}

	public void recAudio(){


		audioactive = true;

		if(audioid == null)
			audioid = new File(notebook.audioMemo, String.format("aud_%s.3gp", System.currentTimeMillis()));

		try{
			recorder.record(audioid.getPath(), audiotv, recButton, playButton, stopButton);

		}
		catch(Exception e){
			audioactive = false;
			return;

		}

	}

	public void playAudio(){
		audioactive = true;

		try{
			recorder.play(this, audioid.getPath(), audiotv, recButton, playButton, stopButton);
		}
		catch(Exception e){
			audioactive = false;
		}		

		//audioactive = false;
	}

	public void stopAudio() {
		audioactive = false;
		try {
			boolean needimage = false;
			if (recorder.recording) {
				needimage = true;
			}
			recorder.stop(audiotv);
			if (needimage && !haveaudioid) {
				addImage(audioid);
				etext.append("\n\n");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		switch (v.getId()) {
		case R.id.cameraButton:
			menu.add(0, 0, 0, "Take a new photo");
			menu.add(0, 1, 0, "Select an existing photo");
			menu.add(0, 2, 0, "Take a new video");
			//menu.add(0, 3, 0, "Select an existing video"
			break;
		case R.id.volumeButton:
			menu.add(0, 4, 0, "Record voice memo");
			menu.add(0, 5, 0, "Voice to text");
			break;
		case R.id.flaskButton:
			menu.add(0, 6, 1, "Calculator");
			menu.add(0, 7, 2, "Stock solution");
			if (RegistrationActivity.isRegistered()) {
				menu.add(0, MENU_LOOKUP, 3, "Lookup");
			}
			break;
		case R.id.penButton:
			menu.add(0, MENU_INC_PEN, 0, "Increase pen size");
			menu.add(0, MENU_DEC_PEN, 0, "Decrease pen size");
			menu.add(0, MENU_RED_PEN, 0, "Use red pen");
			menu.add(0, MENU_GREEN_PEN, 0, "Use green pen");
			menu.add(0, MENU_BLUE_PEN, 0, "Use blue pen");
			break;
		case R.id.eraseButton:
			menu.add(0, MENU_INC_ERASER, 0, "Increase eraser size");
			menu.add(0, MENU_DEC_ERASER, 0, "Decrease eraser size");
			break;
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 0:
			launchCamera();
			return true;
		case 1:
			launchPictureLibrary();
			return true;
		case 2:
			launchVideoCamera();
			return true;
			//		case 3:
			//			launchVideoLibrary();
			//			return true;
		case 4:
			showAudioInterface();
			return true;
		case 5:
			Intent intent = new Intent(this, VoiceRecognition.class);
			intent.putExtra("home", LabBook.labBookDir);
			startActivityForResult(intent, ACTIVITY_SPEECH);
			return true;
		case 6:
			startActivityForResult(new Intent(this, Calculator.class), ACTIVITY_CALCULATOR);
			return true;
		case 7:
			startActivityForResult(new Intent(this, StockSolutionActivity.class), ACTIVITY_STOCKSOLUTION);
			return true;
		case MENU_RED_PEN:
			doverlay.setColor(Color.RED);
			return true;
		case MENU_GREEN_PEN:
			doverlay.setColor(Color.GREEN);
			return true;
		case MENU_BLUE_PEN:
			doverlay.setColor(Color.BLUE);
			return true;
		case MENU_LOOKUP:
			lookup();
			return true;
		}
		return false;
	}

	private void launchPictureLibrary() {
		if (setup()) {
			if (etext.getLineCount() > linenum){
				imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
				keyboardButton.setSelected(false);
				showAddNewPage("End of page reached. Add new page?");
				return;
			}
			Intent intent = new Intent();
			intent.setType("image/*");
			intent.setAction(Intent.ACTION_GET_CONTENT);
			startActivityForResult(Intent.createChooser(intent, "Select Picture"), ACTIVITY_SELECT_PICTURE);
		}
	}

	private void launchCamera() {
		if (setup()) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tmp));
			startActivityForResult(intent, ACTIVITY_CAP_PHOTO);
		}
	}

	private void launchVideoCamera() {
		if (setup()) {
			startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE), ACTIVITY_VIDEO);
		}
	}

	private boolean setup() {
		if (etext.getLineCount() > linenum){
			showAddNewPage("End of page reached. Add new page?");
			return false;
		}
		if (!doverlay.canExpand()) {
			showAddNewPage("End of page reached. Add new page?");
			return false;
		}
		doverlay.candraw = false;
		if (keyboardshowing) {
			hideKeyboard();
		}
		saveData();
		return true;
	}

	private void hideKeyboard() {
		keyboardshowing = false;
		imm.hideSoftInputFromWindow(etext.getWindowToken(), 0);
		keyboardButton.setSelected(false);
	}

	public int getCurrentCursorLine(EditText editText)
	{
		int selectionStart = Selection.getSelectionStart(editText.getText());
		Layout layout = editText.getLayout();

		if (!(selectionStart == -1)) {
			return layout.getLineForOffset(selectionStart);
		}

		return -1;
	}

	private void fillPage() {
		String text = getTextFromFile(page_num);
		//convert legacy notebooks
		if (text != null) {
			text = text.replaceAll("<br>", "\n");
		}
		etext.setText(text);
		if (text != null) {
			addAllImages(text);
		}
		etext.setSelection(0);
		linenum = 52;
		etext.setLines(linenum);
		etext.requestFocus();
		int cursorPosition = LabBook.prefs.getInt(String.format("%s:%s:cursorPosition", notebook.id, page_num), 0);
		if (cursorPosition > 0 && cursorPosition < etext.length()) {
			etext.setSelection(cursorPosition);
		}

		penButton.setSelected(false);
		eraseButton.setSelected(false);

		File bmp = new File(notebook.bmpdir, page_num + ".png");
		if (bmp.exists()){
			doverlay.setBitmap(bmp.getPath());
		} else {
			doverlay.resetBitmap();
		}

		pagetext.setText(String.format("P:%s/%s", page_num, max_page));
	}

	private boolean textModified() {
		return !etext.getText().toString().equals(getTextFromFile(page_num));
	}

	private boolean nameModified() {
		return !notebook.getName().equals(titletext.getText().toString()) && titletext.getText().toString().length() > 0;
	}

	private boolean overlayModified() {
		return doverlay.updated;
	}

	private boolean modified() {
		return textModified() || nameModified() || overlayModified();
	}

	private void thumbnailNote() {
		try {
			File[] screenshots = notebook.screenshots.listFiles();
			if (screenshots.length > 0) {
				Arrays.sort(screenshots);
				String pathName = screenshots[0].getPath();
				Drawable notebookDrawable = getResources().getDrawable(R.drawable.notebook);
				//resize
				Bitmap thumbnail = Util.decodeSampledBitmapFromFile(pathName, notebookDrawable.getIntrinsicWidth(), notebookDrawable.getIntrinsicHeight());
				int width = thumbnail.getWidth();
				int height = (int) (thumbnail.getWidth() * 1.5);
				if (height <= thumbnail.getHeight()) {
					//crop
					thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, width, height);
					//save
					thumbnail.compress(Bitmap.CompressFormat.PNG, 90, new FileOutputStream(new File(getExternalCacheDir(), notebook.id)));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void lookup() {
		int start = etext.getSelectionStart();
		while (start > 0 && Character.isLetterOrDigit(etext.getText().charAt(start - 1))) {
			start--;
		}
		int end = etext.getSelectionStart();
		while (end < etext.length() && Character.isLetterOrDigit(etext.getText().charAt(end))) {
			end++;
		}
		String text = etext.getText().subSequence(start, end).toString();

		if (text.length() > 0) {
			new AsyncTask<String, Void, String>() {
				@Override
				protected String doInBackground(String... params) {
					return Util.lookup(params[0], getResources());
				}
				@Override
				protected void onPostExecute(String result) {
					if (result != null) {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result)));
					} else {
						Toast.makeText(LabBookActivity.this, "Nothing found", Toast.LENGTH_SHORT).show();
					}
				};
			}.execute(text);
		}
	}

	private void lookupBarcode(final String barcode) {
		new AsyncTask<Void, Void, Barcode>() {
			@Override
			protected Barcode doInBackground(Void... params) {
				Barcode result = null;
				if (RegistrationActivity.isRegistered()) {
					result = Util.lookupBarcode(barcode, getResources());
				}
				return result;
			}
			@Override
			protected void onPostExecute(Barcode result) {
				String text;
				if (result != null) {
					File file = new File(notebook.thumbs, String.format("pic_%s.png", barcode));
					try {
						FileOutputStream out = new FileOutputStream(file);
						result.thumbnail.compress(Bitmap.CompressFormat.PNG, 90, out);
						out.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					result.thumbnail.recycle();
					addImage(file);
					text = String.format("%s%n", result.description);
				} else {
					text = String.format("(%s)%n", barcode);
				}
				etext.getText().insert(etext.getSelectionStart(), text);
			}
		}.execute();
	}

}