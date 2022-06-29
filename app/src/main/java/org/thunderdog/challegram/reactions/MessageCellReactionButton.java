package org.thunderdog.challegram.reactions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.Property;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import org.drinkless.td.libcore.telegram.TdApi;
import org.thunderdog.challegram.R;
import org.thunderdog.challegram.charts.CubicBezierInterpolator;
import org.thunderdog.challegram.charts.LayoutHelper;
import org.thunderdog.challegram.data.TD;
import org.thunderdog.challegram.telegram.Tdlib;
import org.thunderdog.challegram.theme.Theme;
import org.thunderdog.challegram.tool.Screen;
import org.thunderdog.challegram.util.text.Counter;
import org.thunderdog.challegram.util.text.TextColorSet;
import org.thunderdog.challegram.widget.ImageReceiverView;

import java.util.Arrays;
import java.util.Random;

import androidx.annotation.NonNull;

public class MessageCellReactionButton extends FrameLayout{

	private ImageReceiverView icon;
	private CounterView counter;
	private boolean selected;
	private Animator currentSelectionAnim;
	private TdApi.MessageReaction reactions;
	private Animator currentTransitionAnim;
	private Tdlib tdlib;
	private ShapeDrawable background;
	private int nonSelectedColor, selectedColor, currentTextColor;
	private int textColorNonSelected, textColorSelected;
	private int currentCount;

	private static final Property<MessageCellReactionButton, Integer> TEXT_COLOR=new Property<>(Integer.class, "fdafsda"){
		@Override
		public Integer get(MessageCellReactionButton object){
			return object.currentTextColor;
		}

		@Override
		public void set(MessageCellReactionButton object, Integer value){
			object.currentTextColor=value;
			object.invalidate();
			object.counter.invalidate();
		}
	};

	public MessageCellReactionButton(@NonNull Context context, Tdlib tdlib){
		super(context);
		this.tdlib=tdlib;
		setWillNotDraw(false);

		setPadding(Screen.dp(7), 0, 0, 0);

		icon=new ImageReceiverView(context);
		addView(icon, LayoutHelper.createFrame(14, 14, Gravity.LEFT | Gravity.CENTER_VERTICAL));

		counter=new CounterView(context, new Counter.Builder().allBold(false).noBackground().textSize(10.5f).colorSet(new TextColorSet(){
			Random rand=new Random();
			@Override
			public int defaultTextColor(){
				return currentTextColor;
			}
		}), 0);
		addView(counter, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 4+14, 0, 9, 0));
	}

	public ImageReceiverView getIcon(){
		return icon;
	}

	private int colorWithAlpha(int color, float alpha){
		return (Math.round(((color >> 24) & 0xff)*alpha) << 24) | (color & 0x00ffffff);
	}

	private ShapeDrawable makeRoundRectDrawable(int color){
		float[] radius=new float[8];
		Arrays.fill(radius, Screen.dp(12));
		ShapeDrawable sd=new ShapeDrawable(new RoundRectShape(radius, null, null));
		sd.getPaint().setColor(color);
		return sd;
	}

	public void setBackgroundStyle(BackgroundStyle style){
		textColorSelected=0xffffffff;
		switch(style){
			case BUBBLE_INCOMING:
				nonSelectedColor=colorWithAlpha(Theme.getColor(R.id.theme_color_bubbleIn_time), .15f);
				selectedColor=Theme.getColor(R.id.theme_color_file);
				textColorNonSelected=Theme.getColor(R.id.theme_color_bubbleIn_text);
				break;
			case BUBBLE_OUTGOING:
				nonSelectedColor=colorWithAlpha(Theme.getColor(R.id.theme_color_bubbleOut_time), .15f);
				selectedColor=Theme.getColor(R.id.theme_color_bubbleOut_file);
				textColorNonSelected=Theme.getColor(R.id.theme_color_bubbleOut_text);
				break;
			case BUBBLE_OUTSIDE:
				nonSelectedColor=colorWithAlpha(Theme.getColor(R.id.theme_color_bubbleIn_time), .15f);
				selectedColor=Theme.getColor(R.id.theme_color_file);
				textColorNonSelected=Theme.getColor(R.id.theme_color_bubbleIn_text);
				break;
			case NO_BUBBLES:
				nonSelectedColor=colorWithAlpha(Theme.getColor(R.id.theme_color_bubbleIn_time), .15f);
				selectedColor=Theme.getColor(R.id.theme_color_file);
				textColorNonSelected=Theme.getColor(R.id.theme_color_bubbleIn_text);
				break;
			default:
				throw new IllegalStateException("Unexpected value: "+style);
		}
		if(background==null){
			background=makeRoundRectDrawable(nonSelectedColor);
			setBackground(background);
		}else{
			background.getPaint().setColor(nonSelectedColor);
		}
		currentTextColor=textColorNonSelected;
		selected=false;
	}

	public void copyFrom(MessageCellReactionButton other){
		setReactions(other.reactions, false);
		setBackground(other.getBackground());
		setForeground(other.getForeground());
		selected=other.selected;
	}

	public void setReactions(TdApi.MessageReaction reaction, boolean animated){
		boolean countChanged=reactions!=null && currentCount!=reaction.totalCount;

		if(!countChanged && reactions!=null && reactions.reaction.equals(reaction.reaction))
			return;

		currentCount=reaction.totalCount;

		reactions=reaction;
		counter.setCount(reaction.totalCount, animated);
		TdApi.Reaction aReaction=tdlib.getReaction(reaction.reaction);
		if(aReaction!=null){
			icon.setVisibility(View.VISIBLE);
			icon.getReceiver().requestFile(TD.toImageFile(tdlib, aReaction.staticIcon.thumbnail));
		}else{
			icon.setVisibility(INVISIBLE);
		}
		if(currentTransitionAnim!=null){
			currentTransitionAnim.cancel();
			currentTransitionAnim=null;
		}
		if(animated && countChanged){
			int prevWidth=getWidth();
			getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
				@Override
				public boolean onPreDraw(){
					getViewTreeObserver().removeOnPreDrawListener(this);

					ObjectAnimator anim=ObjectAnimator.ofFloat(counter, View.TRANSLATION_X, prevWidth-getWidth(), 0f);
					anim.setDuration(220);
					anim.setInterpolator(CubicBezierInterpolator.DEFAULT);
					anim.addListener(new AnimatorListenerAdapter(){
						@Override
						public void onAnimationEnd(Animator animation){
							counter.setTranslationX(0f);
							currentTransitionAnim=null;
						}
					});
					currentTransitionAnim=anim;
					anim.start();

					return true;
				}
			});
		}
	}

	public TdApi.MessageReaction getReaction(){
		return reactions;
	}

	public void setSelected(boolean selected, boolean animated){
		if(selected==this.selected)
			return;
		this.selected=selected;
		if(currentSelectionAnim!=null){
			currentSelectionAnim.cancel();
		}
		if(animated){
			AnimatorSet set=new AnimatorSet();
			set.playTogether(
					ReactionUtils.animateColor(ObjectAnimator.ofInt(background.getPaint(), "color", selected ? selectedColor : nonSelectedColor)),
					ReactionUtils.animateColor(ObjectAnimator.ofInt(this, TEXT_COLOR, selected ? textColorSelected : textColorNonSelected))
			);
			set.setInterpolator(CubicBezierInterpolator.DEFAULT);
			set.setDuration(200);
			set.addListener(new AnimatorListenerAdapter(){
				@Override
				public void onAnimationEnd(Animator animation){
					currentSelectionAnim=null;
				}
			});
			currentSelectionAnim=set;
			set.start();
		}else{
			background.getPaint().setColor(selected ? selectedColor : nonSelectedColor);
			currentTextColor=selected ? textColorSelected : textColorNonSelected;
			counter.invalidate();
			invalidate();
		}
	}

	@Override
	public boolean isSelected(){
		return selected;
	}

	public enum BackgroundStyle{
		BUBBLE_INCOMING,
		BUBBLE_OUTGOING,
		BUBBLE_OUTSIDE,
		NO_BUBBLES
	}
}
