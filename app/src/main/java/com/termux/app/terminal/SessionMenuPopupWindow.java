package com.termux.app.terminal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;
import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

import java.util.ArrayList;
import java.util.List;

public class SessionMenuPopupWindow {

    private final TermuxActivity mActivity;
    private PopupWindow mPopupWindow;
    private RecyclerView mSessionsGrid;
    private SessionsGridAdapter mAdapter;
    private List<TermuxSession> mSessions = new ArrayList<>();

    public SessionMenuPopupWindow(TermuxActivity activity) {
        this.mActivity = activity;
    }

    @SuppressLint("InflateParams")
    public void show() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            return;
        }

        Activity activity = mActivity;
        if (activity == null || activity.isFinishing()) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(activity);
        View popupView = inflater.inflate(R.layout.popup_session_menu, null);

        int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85);
        int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.45);

        mPopupWindow = new PopupWindow(popupView, width, height, true);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.argb(180, 28, 28, 30)));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setAnimationStyle(android.R.style.Animation_Dialog);
        mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);

        // 切换键盘按钮
        popupView.findViewById(R.id.menu_toggle_keyboard_button).setOnClickListener(v -> {
            mActivity.getTermuxTerminalViewClient().onToggleSoftKeyboardRequest();
            dismiss();
        });

        // 新建会话按钮
        popupView.findViewById(R.id.menu_new_session_button).setOnClickListener(v -> {
            mActivity.getTermuxTerminalSessionClient().addNewSession(false, null);
            dismiss();
        });

        // 会话网格
        mSessionsGrid = popupView.findViewById(R.id.menu_sessions_grid);
        mSessionsGrid.setLayoutManager(new GridLayoutManager(activity, 2));
        mAdapter = new SessionsGridAdapter();
        mSessionsGrid.setAdapter(mAdapter);

        // 更新会话数据
        updateSessions();

        // 显示在底部
        mPopupWindow.showAtLocation(activity.getWindow().getDecorView(), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, mActivity.getNavBarHeight() + 30);
    }

    public void dismiss() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
            mPopupWindow = null;
        }
    }

    public boolean isShowing() {
        return mPopupWindow != null && mPopupWindow.isShowing();
    }

    public void updateSessions() {
        if (mActivity.getTermuxService() != null) {
            mSessions = mActivity.getTermuxService().getTermuxSessions();
        }
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    void showSessionOptionsDialog(TerminalSession session) {
        mActivity.getTermuxTerminalSessionClient().renameSession(session);
    }

    class SessionsGridAdapter extends RecyclerView.Adapter<SessionsGridAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_grid, parent, false);
            return new ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TermuxSession termuxSession = mSessions.get(position);
            TerminalSession session = termuxSession.getTerminalSession();

            // 设置序号
            holder.indexText.setText("[" + (position + 1) + "]");

            if (session == null) {
                holder.titleText.setText("null session");
                holder.previewText.setText("");
                // 默认状态：无高亮
                holder.cardView.setStrokeColor(Color.parseColor("#3C3C3E"));
                holder.cardView.setStrokeWidth((int) (1 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
                return;
            }

            String name = session.mSessionName;
            String title = session.getTitle();
            holder.titleText.setText((name != null && !name.isEmpty()) ? name : title);

            // 获取终端内容预览
            String preview = getSessionPreview(termuxSession);
            holder.previewText.setText(preview);

            // 高亮当前选中的会话
            TerminalSession currentSession = mActivity.getCurrentSession();
            boolean isSelected = (currentSession != null) && (session == currentSession);
            if (isSelected) {
                holder.cardView.setStrokeColor(Color.parseColor("#4A90D9"));
                holder.cardView.setStrokeWidth((int) (2 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            } else {
                holder.cardView.setStrokeColor(Color.parseColor("#3C3C3E"));
                holder.cardView.setStrokeWidth((int) (1 * holder.itemView.getContext().getResources().getDisplayMetrics().density));
            }
        }

        private String getSessionPreview(TermuxSession termuxSession) {
            TerminalSession session = termuxSession.getTerminalSession();
            if (session.getEmulator() == null) {
                return "";
            }
            String transcriptText = session.getEmulator().getScreen().getTranscriptText();
            if (transcriptText == null || transcriptText.isEmpty()) {
                return "";
            }
            String[] lines = transcriptText.split("\n");
            StringBuilder sb = new StringBuilder();
            int lineCount = Math.min(4, lines.length);
            int start = lines.length - lineCount;
            for (int i = 0; i < lineCount; i++) {
                String line = lines[start + i];
                if (line != null) {
                    sb.append(line.trim());
                    if (i < lineCount - 1) sb.append("\n");
                }
            }
            return sb.toString().trim();
        }

        @Override
        public int getItemCount() {
            return mSessions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView titleText;
            TextView previewText;
            TextView indexText;
            MaterialCardView cardView;

            ViewHolder(View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.session_title);
                previewText = itemView.findViewById(R.id.session_preview);
                indexText = itemView.findViewById(R.id.session_index);
                cardView = itemView.findViewById(R.id.session_card);

                itemView.setOnClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < mSessions.size()) {
                        TermuxSession termuxSession = mSessions.get(pos);
                        mActivity.getTermuxTerminalSessionClient().setCurrentSession(termuxSession.getTerminalSession());
                        dismiss();
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION && pos < mSessions.size()) {
                        TermuxSession termuxSession = mSessions.get(pos);
                        TerminalSession session = termuxSession.getTerminalSession();
                        if (session != null) {
                            showSessionOptionsDialog(session);
                        }
                    }
                    return true;
                });
            }
        }
    }
}
