package com.example.viewmodel;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.databinding.ObservableField;
import android.os.Bundle;

import com.example.StocksConfig;
import com.example.activity.StockDetailActivity;
import com.example.entity.QuoteEntity;
import com.example.rest.RestHttpLogger;
import com.example.rest.RestResponseHandler;
import com.example.rest.provider.StocksRxServiceProvider;

import org.alfonz.rest.rx.RestRxManager;
import org.alfonz.rx.AlfonzDisposableSingleObserver;
import org.alfonz.utility.NetworkUtility;
import org.alfonz.view.StatefulLayout;

import io.reactivex.Single;
import io.reactivex.observers.DisposableSingleObserver;
import retrofit2.Response;


public class StockDetailRxViewModel extends BaseViewModel implements LifecycleObserver
{
	public final ObservableField<Integer> state = new ObservableField<>();
	public final ObservableField<QuoteEntity> quote = new ObservableField<>();

	private String mSymbol;
	private RestRxManager mRestRxManager = new RestRxManager(new RestResponseHandler(), new RestHttpLogger());


	public StockDetailRxViewModel(Bundle extras)
	{
		// handle intent extras
		handleExtras(extras);
	}


	@OnLifecycleEvent(Lifecycle.Event.ON_START)
	public void onStart()
	{
		// load data
		if(quote.get() == null) loadData();
	}


	@Override
	public void onCleared()
	{
		super.onCleared();

		// unsubscribe
		mRestRxManager.disposeAll();
	}


	public void loadData()
	{
		sendQuote(mSymbol);
	}


	public void refreshData()
	{
		sendQuote(mSymbol);
	}


	public String getChartUrl()
	{
		return String.format(StocksConfig.CHART_BASE_URL, mSymbol);
	}


	private void sendQuote(String symbol)
	{
		if(NetworkUtility.isOnline(getApplicationContext()))
		{
			String callType = StocksRxServiceProvider.QUOTE_CALL_TYPE;
			if(!mRestRxManager.isRunning(callType))
			{
				// show progress
				state.set(StatefulLayout.PROGRESS);

				// subscribe
				Single<Response<QuoteEntity>> rawSingle = StocksRxServiceProvider.getService().quote("json", symbol);
				Single<Response<QuoteEntity>> single = mRestRxManager.setupRestSingleWithSchedulers(rawSingle, callType);
				single.subscribeWith(createQuoteObserver());
			}
		}
		else
		{
			// show offline
			state.set(StatefulLayout.OFFLINE);
		}
	}


	private DisposableSingleObserver<Response<QuoteEntity>> createQuoteObserver()
	{
		return AlfonzDisposableSingleObserver.newInstance(
				response ->
				{
					quote.set(response.body());
					setState(quote);
				},
				throwable ->
				{
					handleError(mRestRxManager.getHttpErrorMessage(throwable));
					setState(quote);
				}
		);
	}


	private void setState(ObservableField<QuoteEntity> data)
	{
		if(data.get() != null)
		{
			state.set(StatefulLayout.CONTENT);
		}
		else
		{
			state.set(StatefulLayout.EMPTY);
		}
	}


	private void handleExtras(Bundle extras)
	{
		if(extras != null)
		{
			mSymbol = extras.getString(StockDetailActivity.EXTRA_SYMBOL);
		}
	}
}
