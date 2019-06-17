package com.nd.frt.timerdemoapp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    public static final int COUNT_DOWN = 10;

    private TextView mCount;
    private int mTime = COUNT_DOWN;
    private Disposable mDisposable;
    private CountDownAsyncTask mCountDownAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCount = findViewById(R.id.tv_count);
        findViewById(R.id.btn_handler).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCount.setText(String.valueOf(mTime));
                        mTime--;
                        if (mTime == 0) {
                            return;
                        }
                        handler.postDelayed(this, 1000);
                    }
                }, 1000);
            }
        });
        findViewById(R.id.btn_asyncTask).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCountDownAsyncTask = new CountDownAsyncTask(MainActivity.this);
                mCountDownAsyncTask.execute();
            }
        });
        findViewById(R.id.btn_rxJava).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDisposable = Flowable.create(new FlowableOnSubscribe<Integer>() {
                    @Override
                    public void subscribe(FlowableEmitter<Integer> emitter) throws Exception {
                        for (int i = 0; i < COUNT_DOWN; i++) {
                            Thread.sleep(1000);
                            emitter.onNext(COUNT_DOWN - i);
                        }
                        emitter.onComplete();
                    }
                }, BackpressureStrategy.BUFFER)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Integer>() {
                            @Override
                            public void accept(Integer integer) throws Exception {
                                mCount.setText(String.valueOf(integer));
                            }
                        });
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mDisposable != null) {
            mDisposable.dispose();
        }
        if (mCountDownAsyncTask != null) {
            mCountDownAsyncTask.cancel(true);
        }
    }

    private static class CountDownAsyncTask extends AsyncTask<Void, Integer, Integer> {

        private final WeakReference<MainActivity> mWeakReferrence;

        public CountDownAsyncTask(MainActivity activity) {
            mWeakReferrence = new WeakReference<>(activity);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                for (int i = 0; i < COUNT_DOWN; i++) {
                    Thread.sleep(1000);
                    publishProgress(COUNT_DOWN + i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            MainActivity mainActivity = mWeakReferrence.get();
            if (mainActivity != null) {
                mainActivity.mCount.setText(String.valueOf(values[0]));
            }
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            MainActivity mainActivity = mWeakReferrence.get();
            if (mainActivity != null) {
                mainActivity.mCount.setText(String.valueOf(integer));
            }
        }
    }
}
