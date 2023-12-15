/***************************************************************************
 *                   (C) Copyright 2003-2023 - Stendhal                    *
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Affero General Public License as        *
 *   published by the Free Software Foundation; either version 3 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 ***************************************************************************/

import { TextBubble } from "./TextBubble";
import { RPEntity } from "../entity/RPEntity";
import { Speech } from "../util/Speech";

declare var stendhal: any;


export class SpeechBubble extends TextBubble {

	private entity: RPEntity;
	private offsetY: number;


	constructor(text: string, entity: RPEntity) {
		text = text.replace(/\\\\/g, "\\");
		super((text.length > 30) ? (text.substring(0, 30) + "...") : text);
		this.entity = entity;

		this.offsetY = 0;
		// find free text bubble position
		const x = this.getX(), y = this.getY();
		for (const bubble of stendhal.ui.gamewindow.textSprites) {
			if (x == bubble.getX() && y + this.offsetY == bubble.getY()) {
				this.offsetY += stendhal.ui.gamewindow.targetTileHeight / 2;
			}
		}

		this.duration = Math.max(
				TextBubble.STANDARD_DUR,
				this.text.length * TextBubble.STANDARD_DUR / 50);
	}

	override draw(ctx: CanvasRenderingContext2D): boolean {
		ctx.lineWidth = 2;
		ctx.font = "14px Arial";
		ctx.fillStyle = '#ffffff';
		ctx.strokeStyle = "#000000";

		if (this.width < 0 || this.height < 0) {
			// get width of text
			this.width = ctx.measureText(this.text).width + 8;
			this.height = 20;
		}

		const x = this.getX(), y = this.getY();
		Speech.drawBubbleRounded(ctx, x, y, this.width, this.height);

		ctx.fillStyle = "#000000";
		ctx.fillText(this.text, x + 4, y + TextBubble.adjustY);

		return this.expired();
	}

	override getX(): number {
		let x = this.entity["_x"] * 32 + (32 * this.entity["width"]);
		if (this.entity.inView()) {
			// keep on screen while entity is visible (border is 1 pixel)
			const overdraw = x + this.width - (stendhal.ui.gamewindow.offsetX + stendhal.ui.gamewindow.width) + 1;
			x = overdraw > 0 ? x - overdraw : x;
		}
		return x;
	}

	override getY(): number {
		let y = this.entity["_y"] * 32 - 16 - (32 * (this.entity["height"] - 1)) + this.offsetY - TextBubble.adjustY;
		if (this.entity.inView()) {
			// keep on screen while entity is visible (border is 1 pixel)
			y = y < stendhal.ui.gamewindow.offsetY + 1 ? stendhal.ui.gamewindow.offsetY + 1 : y;
		}
		return y;
	}
}
