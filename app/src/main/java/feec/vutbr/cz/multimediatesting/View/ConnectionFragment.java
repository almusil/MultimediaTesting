package feec.vutbr.cz.multimediatesting.View;

import android.databinding.DataBindingUtil;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import feec.vutbr.cz.multimediatesting.Contract.ConnectionFragmentContract;
import feec.vutbr.cz.multimediatesting.Loader.PresenterLoader;
import feec.vutbr.cz.multimediatesting.Presenter.ConnectionFragmentPresenter;
import feec.vutbr.cz.multimediatesting.R;
import feec.vutbr.cz.multimediatesting.databinding.ConnectionFragmentBinding;


/**
 * Created by alda on 2.3.17.
 */
public class ConnectionFragment extends Fragment implements ConnectionFragmentContract.View, LoaderManager.LoaderCallbacks<ConnectionFragmentContract.Presenter>, Runnable {

    private static final int LOADER_ID = 2;

    private static final int POSITION = 1;

    private ConnectionFragmentBinding mBind;
    private ConnectionFragmentContract.Presenter mPresenter;

    private Handler mPacketTimer;
    private HandlerThread mHandlerThread;
    private boolean mPacketTimerRunning;
    private int mPacketDelay = 10;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBind = DataBindingUtil.inflate(inflater, R.layout.connection_fragment, container, false);
        return mBind.getRoot();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
        mHandlerThread = new HandlerThread("PacketHandler");
        mHandlerThread.start();
        mPacketTimer = new Handler(mHandlerThread.getLooper());

    }


    @Override
    public void onResume() {
        super.onResume();
        mPresenter.onAttachView(this);
        mBind.setPresenter(mPresenter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPresenter.onDetachView();
    }

    @Override
    public void onStop() {
        super.onStop();
        mHandlerThread.quit();
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new PresenterLoader(getActivity(), new ConnectionFragmentPresenter.Factory());
    }

    @Override
    public void onLoadFinished(Loader<ConnectionFragmentContract.Presenter> loader, ConnectionFragmentContract.Presenter data) {
        mPresenter = data;
    }


    @Override
    public void onLoaderReset(Loader loader) {
        mPresenter = null;
    }


    @Override
    public void hideButton() {
        mBind.btnStart.setVisibility(View.GONE);
    }

    @Override
    public void showButton() {
        mBind.btnStart.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        mBind.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void showLoading() {
        mBind.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void postInfo(final String info) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBind.infoText.setText(info);
            }
        });
    }

    @Override
    public void startTimer() {
        mPacketTimerRunning = true;
        mPacketTimer.postDelayed(this, mPacketDelay);
    }

    @Override
    public void stopTimer() {
        mPacketTimerRunning = false;
        mPacketTimer.removeCallbacks(this);
    }

    @Override
    public void run() {
        mPresenter.onSendNewPacket();
        if (mPacketTimerRunning) {
            mPacketTimer.postDelayed(this, mPacketDelay);
        }
    }
}
