/*
 * This file is part of Chiaki.
 *
 * Chiaki is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chiaki is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Chiaki.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.metallic.chiaki.touchcontrols

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.metallic.chiaki.R
import kotlin.math.PI
import kotlin.math.atan2

class DPadView @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr)
{
	enum class Direction {
		LEFT,
		RIGHT,
		UP,
		DOWN,
		LEFT_UP,
		RIGHT_UP,
		LEFT_DOWN,
		RIGHT_DOWN;

		val isDiagonal get() = when(this) {
			LEFT_UP, RIGHT_UP, LEFT_DOWN, RIGHT_DOWN -> true
			else -> false
		}
	}

	var state: Direction? = null
		private set

	var stateChangeCallback: ((Direction?) -> Unit)? = null

	/**
	 * Radius (as a fraction of the entire DPad Radius)
	 * to be used as a deadzone in the center on move events
	 */
	private val deadzoneRadius = 0.3f

	private val dpadIdleDrawable = VectorDrawableCompat.create(resources, R.drawable.control_dpad_idle, null)
	private val dpadLeftDrawable = VectorDrawableCompat.create(resources, R.drawable.control_dpad_left, null)
	private val dpadLeftUpDrawable = VectorDrawableCompat.create(resources, R.drawable.control_dpad_left_up, null)

	private val touchTracker = TouchTracker().also {
		it.positionChangedCallback = this::updateState
	}

	override fun onDraw(canvas: Canvas)
	{
		super.onDraw(canvas)

		val state = state
		val drawable: VectorDrawableCompat?
		if(state != null)
		{
			drawable = if(state.isDiagonal) dpadLeftUpDrawable else dpadLeftDrawable
			when(state)
			{
				Direction.UP, Direction.RIGHT_UP -> canvas.rotate(90f, width.toFloat() * 0.5f, height.toFloat() * 0.5f)
				Direction.DOWN, Direction.LEFT_DOWN -> canvas.rotate(90f*3f, width.toFloat() * 0.5f, height.toFloat() * 0.5f)
				Direction.LEFT, Direction.LEFT_UP -> {}
				Direction.RIGHT, Direction.RIGHT_DOWN -> canvas.rotate(90f*2f, width.toFloat() * 0.5f, height.toFloat() * 0.5f)
			}
		}
		else
			drawable = dpadIdleDrawable

		drawable?.setBounds(0, 0, width, height)
		//drawable?.alpha = 127
		drawable?.draw(canvas)
	}

	private fun directionForPosition(position: Vector): Direction
	{
		val dir = (position / Vector(width.toFloat(), height.toFloat()) - 0.5f) * 2.0f
		val angleSection = PI.toFloat() * 2.0f / 8.0f
		val angle = atan2(dir.x, dir.y) + PI + angleSection * 0.5f
		return when
		{
			angle < 1.0f * angleSection -> Direction.UP
			angle < 2.0f * angleSection -> Direction.LEFT_UP
			angle < 3.0f * angleSection -> Direction.LEFT
			angle < 4.0f * angleSection -> Direction.LEFT_DOWN
			angle < 5.0f * angleSection -> Direction.DOWN
			angle < 6.0f * angleSection -> Direction.RIGHT_DOWN
			angle < 7.0f * angleSection -> Direction.RIGHT
			angle < 8.0f * angleSection -> Direction.RIGHT_UP
			else -> Direction.UP
		}
	}

	private fun updateState(position: Vector?)
	{
		val newState =
			if(position == null)
				null
			else
			{
				val xFrac = 2.0f * (position.x / width.toFloat() - 0.5f)
				val yFrac = 2.0f * (position.y / height.toFloat() - 0.5f)
				val radiusSq = xFrac * xFrac + yFrac * yFrac
				if(radiusSq < deadzoneRadius * deadzoneRadius && state != null)
					state
				else
					directionForPosition(position)
			}

		if(state != newState)
		{
			state = newState
			invalidate()
			stateChangeCallback?.let { it(state) }
		}
	}

	override fun onTouchEvent(event: MotionEvent): Boolean
	{
		touchTracker.touchEvent(event)
		return true
	}

}