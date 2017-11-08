package com.wilin.texturedemo;

import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wilin.texturedemo.textureView.VideoDisplayView;

import java.io.File;

/**
 * Created by Lin WenLong on 2017/11/1.
 * <p>
 * Video display in List.
 */
class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoHolder> {
    private static final String TAG = "VideoAdapter";

    VideoAdapter() {

    }

    @Override
    public int getItemCount() {
        return 10;
    }

    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VideoHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false));
    }

    @Override
    public void onBindViewHolder(VideoHolder holder, int position) {
        holder.videoDisplayView.setVideoPath(Environment.getExternalStorageDirectory().getPath()
                .concat(File.separator).concat(String.valueOf(position)).concat(".mp4"));
        holder.playIv.setVisibility(View.VISIBLE);
        holder.pauseIv.setVisibility(View.GONE);
    }

    static class VideoHolder extends RecyclerView.ViewHolder implements
            VideoDisplayView.OnVisibleChangeListener{
        private VideoDisplayView videoDisplayView;
        private ImageView playIv;
        private ImageView pauseIv;

        @Override
        public void onVisibleChanged(int visibility) {
            if (visibility == View.VISIBLE) {
                playIv.setVisibility(View.VISIBLE);
                pauseIv.setVisibility(View.GONE);
            }
        }

        VideoHolder(View view) {
            super(view);
            videoDisplayView = (VideoDisplayView) view.findViewById(R.id.video_display_view);
            videoDisplayView.setOnVisibleChangeListener(this);
            playIv = (ImageView) view.findViewById(R.id.play_iv);
            pauseIv = (ImageView) view.findViewById(R.id.pause_iv);
            playIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    videoDisplayView.startPlay();
                    pauseIv.setVisibility(View.VISIBLE);
                    playIv.setVisibility(View.GONE);
                }
            });
            pauseIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    videoDisplayView.pausePlay();
                    playIv.setVisibility(View.VISIBLE);
                    pauseIv.setVisibility(View.GONE);
                }
            });
        }
    }


}
