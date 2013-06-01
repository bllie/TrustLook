package com.trustlook.app;

import java.util.List;

import android.widget.ArrayAdapter;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
		
		int width = this.getContext().getResources().getDisplayMetrics().widthPixels;
		labelView.setMaxWidth(width - 200);
		detailView.setMaxWidth(width - 200);
		Log.d(TAG, "width = " + width);
		
		labelView.setTypeface(PkgUtils.getRegularFont());
		detailView.setTypeface(PkgUtils.getRegularFont());
		riskTextView.setTypeface(PkgUtils.getRegularFont());
		
		String virusName = objects.get(position).getVirusName();
		String summary = objects.get(position).getSummary();
		
		labelView.setText(objects.get(position).getDisplayName());		
		detailView.setText(((virusName != null) ? virusName : "") + "\n" + ((summary != null) ? summary : ""));
		
//		Log.d(TAG, "rowView - width: " + rowView.getWidth() + ", height: " + rowView.getHeight());
//		Log.d(TAG, "imageView - width: " + imageView.getWidth() + ", height: " + imageView.getHeight());
		
		
		String scoreString = objects.get(position).getScore();
		PkgUtils.RISK_LEVEL riskLevel = PkgUtils.getRiskLevel(scoreString);
		String riskText = "L";
		int backgroundColor = Color.GRAY;
		GradientDrawable bgShape = (GradientDrawable)riskTextView.getBackground();
		switch (riskLevel) {
			case HIGH:
				riskText = "H";
				backgroundColor = Color.parseColor("#EE8B8B");
				break;
			case MEDIUM:
				riskText = "M";
				backgroundColor = Color.parseColor("#F7C98B");
				break;
			case LOW:
				riskText = "L";
				backgroundColor = Color.parseColor("#0CBA98");
				break;
			default:
				break;
		}
		bgShape.setColor(backgroundColor);
		riskTextView.setText(riskText);
   
		Drawable icon = objects.get(position).getIcon();
		
		if (icon != null)
			imageView.setImageDrawable(objects.get(position).getIcon());
		else
			imageView.setImageResource(R.drawable.windowsmobile_logo);
		
		return rowView;
	}
}
