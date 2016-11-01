package me.imli.lib_moretextlayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Doots on 2016/10/31.
 */
public class MoreTextLayout extends LinearLayout implements View.OnClickListener {

    private static final String TAG = "MoreTextLayout";

    /**
     * 展开/收缩监听器
     */
    private OnExpandListener mOnExpandListener;


    /**
     * TextView
     */
    private TextView textView;

    /**
     * 收起/全部TextView
     * <br>shrink/expand TextView
     */
    private TextView tvState;

    /**
     * 点击进行折叠/展开的图片
     * <br>shrink/expand icon
     */
    private ImageView ivExpandOrShrink;

    /**
     * 底部是否折叠/收起的父类布局
     * <br>shrink/expand layout parent
     */
    private RelativeLayout rlToggleLayout;

    /**
     * 提示折叠的图片资源
     * <br>shrink drawable
     */
    private Drawable drawableShrink;
    /**
     * 提示显示全部的图片资源
     * <br>expand drawable
     */
    private Drawable drawableExpand;

    /**
     * 全部/收起文本的字体颜色
     * <br>color of shrink/expand text
     */
    private int textViewStateColor;
    /**
     * 展开提示文本
     * <br>expand text
     */
    private String textExpand;
    /**
     * 收缩提示文本
     * <br>shrink text
     */
    private String textShrink;

    /**
     * 是否折叠显示的标示
     * <br>flag of shrink/expand
     */
    private boolean isShrink = false;

    /**
     * 是否需要折叠的标示
     * <br>flag of expand needed
     */
    private boolean isExpandNeeded = false;

    /**
     * 是否初始化TextView
     * <br>flag of TextView Initialization
     */
    private boolean isInitTextView = true;

    /**
     * 折叠显示的行数
     * <br>number of lines to expand
     */
    private int expandLines;

    /**
     * 文本的行数
     * <br>Original number of lines
     */
    private int textLines;

    /**
     * 动画线程
     * <br>thread
     */
    private Thread thread;

    /**
     * 动画过度间隔
     * <br>animation interval
     */
    private int sleepTime = 22;

    /**
     * 是否在动画中
     */
    private boolean isAnim = false;

    /**
     * handler信号
     * <br>handler signal
     */
    private final int WHAT = 2;
    /**
     * 动画结束信号
     * <br>animation end signal of handler
     */
    private final int WHAT_ANIMATION_END = 3;

    /**
     * 动画结束，只是改变图标，并不隐藏
     * <br>animation end and expand only,but not disappear
     */
    private final int WHAT_EXPAND_ONLY = 4;

    public MoreTextLayout(Context context) {
        this(context, null);
    }

    public MoreTextLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoreTextLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.initValue(attrs);
    }

    private void initValue(AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.MoreTextLayout);

        expandLines = ta.getInteger(R.styleable.MoreTextLayout_mtlExpandLines, 5);
        drawableShrink = ta.getDrawable(R.styleable.MoreTextLayout_mtlShrinkBitmap);
        drawableExpand = ta.getDrawable(R.styleable.MoreTextLayout_mtlExpandBitmap);

        textViewStateColor = ta.getColor(R.styleable.MoreTextLayout_mtlTextStateColor, ContextCompat.getColor(getContext(), android.R.color.black));

        textShrink = ta.getString(R.styleable.MoreTextLayout_mtlTextShrink);
        textExpand = ta.getString(R.styleable.MoreTextLayout_mtlTextExpand);

        if (null == drawableShrink) {
            drawableShrink = ContextCompat.getDrawable(getContext(), R.drawable.icon_green_arrow_up);
        }

        if (null == drawableExpand) {
            drawableExpand = ContextCompat.getDrawable(getContext(), R.drawable.icon_green_arrow_down);
        }

        if (TextUtils.isEmpty(textShrink)) {
            textShrink = getContext().getString(R.string.shrink);
        }

        if (TextUtils.isEmpty(textExpand)) {
            textExpand = getContext().getString(R.string.expand);
        }

        ta.recycle();
    }

    private void initView() {
        rlToggleLayout = (RelativeLayout) findViewById(R.id.rl_expand_text_view_animation_toggle_layout);

        // TextView
        textView = (TextView) getChildAt(0);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.addTextChangedListener(onTextWatcher());
        addTvPreDrawListener();

        // 图标
        ivExpandOrShrink = (ImageView) findViewById(R.id.iv_expand_text_view_animation_toggle);

        // Text State
        tvState = (TextView) findViewById(R.id.tv_expand_text_view_animation_hint);
        tvState.setTextColor(textViewStateColor);
    }

    private void initClick() {
        textView.setOnClickListener(this);
        rlToggleLayout.setOnClickListener(this);
    }

    /**
     * 添加绘制监听
     */
    private void addTvPreDrawListener() {
        ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
        viewTreeObserver.addOnPreDrawListener(onPreDrawListener);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (WHAT == msg.what) {
                textView.setMaxLines(msg.arg1);
                textView.invalidate();
            } else if (WHAT_ANIMATION_END == msg.what) {
                setExpandState(msg.arg1);
            } else if (WHAT_EXPAND_ONLY == msg.what) {
                changeExpandState(msg.arg1);
            }
            super.handleMessage(msg);
        }

    };

    /**
     * @param startIndex 开始动画的起点行数 <br> start index of animation
     * @param endIndex   结束动画的终点行数 <br> end index of animation
     * @param what       动画结束后的handler信号标示 <br> signal of animation end
     */
    private void doAnimation(final int startIndex, final int endIndex,
                             final int what) {

        thread = new Thread(new Runnable() {

            @Override
            public void run() {

                // 标记动画开始
                isAnim = true;

                // 开始动画
                if (startIndex < endIndex) {
                    // 如果起止行数小于结束行数，那么往下展开至结束行数
                    // if start index smaller than end index ,do expand action
                    int count = startIndex;
                    while (count++ < endIndex) {
                        Message msg = handler.obtainMessage(WHAT, count, 0);

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                } else if (startIndex > endIndex) {
                    // 如果起止行数大于结束行数，那么往上折叠至结束行数
                    // if start index bigger than end index ,do shrink action
                    int count = startIndex;
                    while (count-- > endIndex) {
                        Message msg = handler.obtainMessage(WHAT, count, 0);
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        handler.sendMessage(msg);
                    }
                }

                // 动画结束后发送结束的信号
                // animation end,send signal
                Message msg = handler.obtainMessage(what, endIndex, 0);
                handler.sendMessage(msg);

                // 标记动画结束
                isAnim = false;
            }

        });

        thread.start();

    }


    /**
     * 改变折叠状态（仅仅改变折叠与展开状态，不会隐藏折叠/展开图片布局）
     * change shrink/expand state(only change state,but not hide shrink/expand icon)
     *
     * @param endIndex
     */
    @SuppressWarnings("deprecation")
    private void changeExpandState(int endIndex) {
        rlToggleLayout.setVisibility(View.VISIBLE);
        if (endIndex < textLines) {
            ivExpandOrShrink.setBackgroundDrawable(drawableExpand);
            tvState.setText(textExpand);
        } else {
            ivExpandOrShrink.setBackgroundDrawable(drawableShrink);
            tvState.setText(textShrink);
        }
    }

    /**
     * 设置折叠状态（如果折叠行数设定大于文本行数，那么折叠/展开图片布局将会隐藏,文本将一直处于展开状态）
     * change shrink/expand state(if number of expand lines bigger than original text lines,hide shrink/expand icon,and TextView will always be at expand state)
     *
     * @param endIndex
     */
    @SuppressWarnings("deprecation")
    private void setExpandState(int endIndex) {
        if (endIndex < textLines) {
            isShrink = true;
            rlToggleLayout.setVisibility(View.VISIBLE);
            ivExpandOrShrink.setBackgroundDrawable(drawableExpand);
            textView.setOnClickListener(this);
            tvState.setText(textExpand);
        } else {
            isShrink = false;
            rlToggleLayout.setVisibility(View.GONE);
            ivExpandOrShrink.setBackgroundDrawable(drawableShrink);
            textView.setOnClickListener(null);
            tvState.setText(textShrink);
        }

    }


    /**
     * 获取动画过度时间
     * @return
     */
    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * 设置动画过度时间
     * @param sleepTime
     */
    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // 查找布局的 TextView
        View textView = getChildAt(0);
        if (!(textView instanceof TextView)) {
            throw new RuntimeException("The first view must be a 'TextView'!");
        } else {
            this.textView = (TextView) textView;
        }
        // 扩展布局 View
        View exView = View.inflate(getContext(), R.layout.layout_expand, null);

        // 添加 view 到布局
        addView(exView, new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // init
        initView();
        initClick();

        // 强制刷新
        postInvalidate();
    }

    /**
     * 点击扩展按钮
     */
    private void clickToggle() {
        if (isAnim) return;
        if (isShrink) {
            // 如果是已经折叠，那么进行非折叠处理
            // do shrink action
            doAnimation(expandLines, textLines, WHAT_EXPAND_ONLY);
        } else {
            // 如果是非折叠，那么进行折叠处理
            // do expand action
            doAnimation(textLines, expandLines, WHAT_EXPAND_ONLY);
        }

        // 切换状态
        // set flag
        isShrink = !isShrink;
    }

    /**
     * 无需折叠
     * do not expand
     */
    private void doNotExpand() {
        textView.setMaxLines(expandLines);
        rlToggleLayout.setVisibility(View.GONE);
        textView.setOnClickListener(null);
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.rl_expand_text_view_animation_toggle_layout || v == this.textView) {
            clickToggle();
        }
    }

    /**
     * TextView 监听
     * @return
     */
    private TextWatcher onTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                addTvPreDrawListener();
            }
        };
    }

    /**
     * TextView 绘制监听, 设置展开/收缩
     */
    private ViewTreeObserver.OnPreDrawListener onPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (!isInitTextView) {
                return true;
            }
            textLines = textView.getLineCount();
            isExpandNeeded = textLines > expandLines;
            isInitTextView = false;
            if (isExpandNeeded) {
                isShrink = true;
                doAnimation(textLines, expandLines, WHAT_ANIMATION_END);
            } else {
                isShrink = false;
                doNotExpand();
            }
            return true;
        }
    };



    /**
     * 设置监听
     * @param listener
     */
    public void setOnExpandListener(OnExpandListener listener) {
        mOnExpandListener = listener;
    }


    /**
     * 展开监听
     */
    public interface OnExpandListener {

        /**
         * 打开
         * @param view
         */
        public void onOpen(View view);

        /**
         * 关闭
         * @param view
         */
        public void onClose(View view);
    }
}
