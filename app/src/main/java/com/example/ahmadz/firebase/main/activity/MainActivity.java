package com.example.ahmadz.firebase.main.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.ahmadz.firebase.R;
import com.example.ahmadz.firebase.main.adapter.OrderItemRecyclerAdapter;
import com.example.ahmadz.firebase.main.callback.OrderItemChangedListener;
import com.example.ahmadz.firebase.main.database.FireBaseHelper;
import com.example.ahmadz.firebase.main.model.OrderItemMetaInfo;
import com.example.ahmadz.firebase.main.model.OrderItemViewHolder;
import com.facebook.FacebookSdk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements OrderItemChangedListener {

	@Bind(R.id.toolbar) Toolbar toolbar;
	@Bind(R.id.recyclerView_orders) RecyclerView recyclerOrders;
	@Bind(R.id.empty_message) TextView emptyMessage;
	@Bind(R.id.progress_bar) ProgressBar progressBar;

	private final String TAG = this.getClass().getSimpleName();
	private String TODAY;
	private Context mContext;
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener authListener;
	private DatabaseReference mDatabase;
	private OrderItemRecyclerAdapter mAdapter;
	private String userUID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		setSupportActionBar(toolbar);
		mContext = this;
		TODAY = getDate();
		//initialize Facebook SDK.
		FacebookSdk.sdkInitialize(getApplicationContext());

		setupAuthentication();

		if(mAuth.getCurrentUser() != null) {//if signed in.
			setupRecyclerViewSync();
		}
	}

	private String getDate() {
		Calendar cal = Calendar.getInstance();
		return String.format("%s-%s-%s", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH)+1, cal.get(Calendar.YEAR));
	}

	@Override
	public void onStart() {
		super.onStart();
		mAuth.addAuthStateListener(authListener);
	}

	@OnClick(R.id.fab)
	public void fabClicked(){
		new MaterialDialog.Builder(mContext)
				.title("Add Order Item!")
				.titleGravity(GravityEnum.CENTER)
				.titleColor(getResources().getColor(R.color.colorPrimaryDark))
				.inputType(InputType.TYPE_CLASS_TEXT)
				.input("Order Item Name...", "", (dialog, input) -> {
					String itemName = input.toString();
					addNewItem(itemName, 1);
				})
				.show();
	}

	private void addNewItem(String itemName, int quantity) {
		OrderItemMetaInfo orderItemMetaInfo = new OrderItemMetaInfo(itemName, quantity);
		mDatabase.child(TODAY).child(getString(R.string.orders_node)).push().setValue(orderItemMetaInfo);
	}

	private void startDefaultOrderActivity() {
		Intent intent = new Intent(mContext, DefaultOrderActivity.class);
		intent.putExtra(getString(R.string.uid), userUID);
		startActivity(intent);
	}

	private void setupRecyclerViewSync() {
		progressBar.setVisibility(View.VISIBLE);
		recyclerOrders.setHasFixedSize(true);
		recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

		userUID = mAuth.getCurrentUser().getUid();
		mDatabase = FireBaseHelper.getDatabase()
				.getReference();

		mAdapter = new OrderItemRecyclerAdapter(
				mContext,
				this,
				OrderItemMetaInfo.class,
				R.layout.order_item_layout,
				OrderItemViewHolder.class,
				mDatabase.child(TODAY).child(getString(R.string.orders_node))
		);
		recyclerOrders.setAdapter(mAdapter);

		// for data tracking.
		mDatabase.child(TODAY).child(getString(R.string.orders_node))
				.addValueEventListener(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot dataSnapshot) {
						progressBar.setVisibility(View.INVISIBLE);
						if (dataSnapshot == null || dataSnapshot.getChildrenCount() == 0){
							emptyMessage.setVisibility(View.VISIBLE);
						}else{
							emptyMessage.setVisibility(View.INVISIBLE);
						}
					}
					@Override
					public void onCancelled(DatabaseError databaseError) {

					}
				});
	}

	private void setupAuthentication() {
		mAuth = FirebaseAuth.getInstance();

		authListener = fireBaseAuth -> {
			Log.i(TAG, "setupAuthentication: done");
			FirebaseUser user = fireBaseAuth.getCurrentUser();
			if (user == null) {
				startActivity(new Intent(MainActivity.this, LoginActivity.class));
				finish();
			}
		};
	}

	@Override
	public void onQuantityChanged(int position, int quantity) {
		if (quantity == 0){
			mAdapter.getRef(position).removeValue();
			return;
		}
		Map<String, Object> map = new HashMap<>();
		map.put(getString(R.string.quantity), quantity);
		mAdapter.getRef(position).updateChildren(map);
	}

	private void signOut() {
		new MaterialDialog.Builder(this)
				.title("Log-Out")
				.content("Are you sure you wanna Logout?")
				.positiveText("Yes")
				.negativeText("Cancel")
				.onPositive((dialog, which) -> signMeOut())
				.show();
	}

	private void signMeOut(){
		mAuth.signOut();
		// TODO: 7/23/16 logout from facebook
	}

	@Override
	public void onStop() {
		super.onStop();
		if (authListener != null) {
			mAuth.removeAuthStateListener(authListener);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.action_signout:
				signOut();
				break;
			case R.id.default_order:
				startDefaultOrderActivity();
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
