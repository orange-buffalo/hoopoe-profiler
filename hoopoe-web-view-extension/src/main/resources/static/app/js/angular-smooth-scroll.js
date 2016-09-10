/*!
 *	 Angular Smooth Scroll (ngSmoothScroll)
 *	 Animates scrolling to elements, by David Oliveros.
 *
 *   Callback hooks contributed by Ben Armston https://github.com/benarmston
 *	 Easing support contributed by Willem Liu. https://github.com/willemliu
 *	 Easing functions forked from Gaëtan Renaudeau. https://gist.github.com/gre/1650294
 *	 Infinite loop bugs in iOS and Chrome (when zoomed) by Alex Guzman. https://github.com/alexguzman
 *	 Support for scrolling in custom containers by Joseph Matthias Goh. https://github.com/zephinzer
 *	 Influenced by Chris Ferdinandi
 *	 https://github.com/cferdinandi
 *
 *	 Version: 2.0.0
 * 	 License: MIT
 */

(function () {
	'use strict';

	var module = angular.module('smoothScroll', []);


	/**
	 * Smooth scrolls the window/div to the provided element.
	 *
	 * 20150713 EDIT - zephinzer
	 * 	Added new option - containerId to account for scrolling within a DIV
	 */
	var smoothScroll = function (element, options) {
		options = options || {};

		// Options
		var duration = options.duration || 800,
			offsetV = options.offsetV || 0,
			offsetH = options.offsetH || 0,
			easing = options.easing || 'easeInOutQuart',
			callbackBefore = options.callbackBefore || function() {},
			callbackAfter = options.callbackAfter || function() {},
			container = document.getElementById(options.containerId) || null,
			containerPresent = (container != undefined && container != null);

		/**
		 * Retrieve current location
		 */
		var getScrollLocation = function() {
			if(containerPresent) {
				return {
				  v: container.scrollTop,
          h: container.scrollLeft
        };
			} else {
				if(window.pageYOffset) {
					return {
					  v: window.pageYOffset,
            h: window.pageXOffset
          };
				} else {
					return {
					  v: document.documentElement.scrollTop,
            h: document.documentElement.scrollLeft
          };
				}
			}
		};

		/**
		 * Calculate easing pattern.
		 *
		 * 20150713 edit - zephinzer
		 * - changed if-else to switch
		 * @see http://archive.oreilly.com/pub/a/server-administration/excerpts/even-faster-websites/writing-efficient-javascript.html
		 */
		var getEasingPattern = function(type, time) {
			switch(type) {
				case 'easeInQuad': 		return time * time; // accelerating from zero velocity
				case 'easeOutQuad': 	return time * (2 - time); // decelerating to zero velocity
				case 'easeInOutQuad': 	return time < 0.5 ? 2 * time * time : -1 + (4 - 2 * time) * time; // acceleration until halfway, then deceleration
				case 'easeInCubic': 	return time * time * time; // accelerating from zero velocity
				case 'easeOutCubic': 	return (--time) * time * time + 1; // decelerating to zero velocity
				case 'easeInOutCubic': 	return time < 0.5 ? 4 * time * time * time : (time - 1) * (2 * time - 2) * (2 * time - 2) + 1; // acceleration until halfway, then deceleration
				case 'easeInQuart': 	return time * time * time * time; // accelerating from zero velocity
				case 'easeOutQuart': 	return 1 - (--time) * time * time * time; // decelerating to zero velocity
				case 'easeInOutQuart': 	return time < 0.5 ? 8 * time * time * time * time : 1 - 8 * (--time) * time * time * time; // acceleration until halfway, then deceleration
				case 'easeInQuint': 	return time * time * time * time * time; // accelerating from zero velocity
				case 'easeOutQuint': 	return 1 + (--time) * time * time * time * time; // decelerating to zero velocity
				case 'easeInOutQuint': 	return time < 0.5 ? 16 * time * time * time * time * time : 1 + 16 * (--time) * time * time * time * time; // acceleration until halfway, then deceleration
				default:				return time;
			}
		};

		/**
		 * Calculate how far to scroll
		 */
		var getEndLocation = function(element) {
			var location = {
			  v: 0,
			  h: 0
      };
			if (element.offsetParent) {
				do {
					location.v += element.offsetTop;
					location.h += element.offsetLeft;
					element = element.offsetParent;
				} while (element);
			}
			location.v = Math.max(location.v - offsetV, 0);
			location.h = Math.max(location.h - offsetH, 0);
			return location;
		};

		// Initialize the whole thing
		setTimeout( function() {
			var currentLocation = null,
				startLocation 	= getScrollLocation(),
				endLocation 	= getEndLocation(element),
				timeLapsed 		= 0,
				distance 		= {
				  v: endLocation.v - startLocation.v,
				  h: endLocation.h - startLocation.h
        },
				percentage,
				position,
				scrollHeight,
				internalHeight;

			/**
			 * Stop the scrolling animation when the anchor is reached (or at the top/bottom of the page)
			 */
			var stopAnimation = function () {
				currentLocation = getScrollLocation();
				if(containerPresent) {
					scrollHeight = container.scrollHeight;
					internalHeight = container.clientHeight + currentLocation.v;
				} else {
					scrollHeight = document.body.scrollheight;
					internalHeight = window.innerHeight + currentLocation.v;
				}

				if (
					( // condition 1
						position.v == endLocation.v
					) ||
					( // condition 2
						currentLocation.v == endLocation.v
					) ||
					( // condition 3
						internalHeight >= scrollHeight
					)
				) { // stop
					clearInterval(runAnimation);
					callbackAfter(element);
				}
			};

			/**
			 * Scroll the page by an increment, and check if it's time to stop
			 */
			var animateScroll = function () {
				timeLapsed += 16;
				percentage = ( timeLapsed / duration );
				percentage = ( percentage > 1 ) ? 1 : percentage;
				position = {
				  v: startLocation.v + ( distance.v * getEasingPattern(easing, percentage) ),
				  h: startLocation.h + ( distance.h * getEasingPattern(easing, percentage) )
        };
				if(containerPresent) {
					container.scrollTop = position.v;
					container.scrollLeft = position.h;
				} else {
					window.scrollTo( position.h, position.v );
				}
				stopAnimation();
			};

			callbackBefore(element);
			var runAnimation = setInterval(animateScroll, 16);
		}, 0);
	};

	// Expose the library in a factory
	//
	module.factory('smoothScroll', function() {
		return smoothScroll;
	});

}());