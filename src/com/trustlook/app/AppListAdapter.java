package com.trustlook.app;

import java.util.List;

import android.widget.ArrayAdapter;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.*;
import android.util.Log;
import android.view.*;
import com.trustlook.app.R;

public class AppListAdapter extends ArrayAdapter<AppInfo> {
	private final String TAG = "TL";
	private final Context context;
	private List<AppInfo> objects;
		
	public AppListAdapter(Context context, List<AppInfo> objects) {
		super(context, R.layout.list_item, objects);

		this.context = context;
		this.objects = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
		View rowView = inflater.inflate(R.layout.list_item, parent, false);
		TextView labelView = (TextView) rowView.findViewById(R.id.appLabel);
		TextView detailView = (TextView)rowView.findViewById(R.id.appDetails);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.appLogo);
		TextView riskTextView = (TextView) rowView.findViewById(R.id.riskTextView);
		
		Typeface appFont = Typeface.createFromAsset(context.getAssets(), "MyriadPro-Light.otf");
		labelView.setTypeface(appFont);
		detailView.setTypeface(appFont);
		riskTextView.setTypeface(appFont);
		
		labelView.setText(objects.get(position).getDisplayName());		
		detailView.setText(objects.get(position).getVirusName() + "\n" + objects.get(position).getSummary());
		
		Log.d(TAG, "rowView - width: " + rowView.getWidth() + ", height: " + rowView.getHeight());
		Log.d(TAG, "imageView - width: " + imageView.getWidth() + ", height: " + imageView.getHeight());
//		labelView.setWidth(400);
//		detailView.setWidth(400);
		
		String scoreString = objects.get(position).getScore();
		int scoreInt = 0;
		try {
			scoreInt = Math.round(Float.parseFloat(scoreString));
		}
		catch (Exception e) {
			scoreInt = 0;
		}
		riskTextView.setText("" + scoreInt);
   
		Drawable icon = objects.get(position).getIcon();
		
		if (icon != null)
			imageView.setImageDrawable(objects.get(position).getIcon());
		else
			imageView.setImageResource(R.drawable.windowsmobile_logo);
		
		return rowView;
	}
}
