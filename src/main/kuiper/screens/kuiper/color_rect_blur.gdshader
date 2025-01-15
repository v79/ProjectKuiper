shader_type canvas_item;
render_mode unshaded;

/**
 * How blurry the result should be.
 * This value will cause the shader to sample each pixel's neighbors up to `strength` pixels away and blur them together.
 */
uniform float strength : hint_range(0.0, 10.0) = 0.0;

/**
 * Exponent value for the number of samples to take.
 * This value affects the quality at the cost of performance.
 * The number of samples taken will be 2 to the power of this value, which means that every time you increment this value by one it doubles the required processing time.
 */
uniform int sample_power : hint_range(3, 8, 1) = 4;

/**
 * Check this box when using this on a CanvasGroup.
 */
uniform bool is_canvas_group = false;

uniform sampler2D screen_texture : hint_screen_texture, repeat_disable, filter_nearest;

vec2 rotate(vec2 v, float angle) {
	mat2 rotation_matrix = mat2(vec2(cos(angle), sin(angle)), vec2(-sin(angle), cos(angle)));
    return v * rotation_matrix;
}

void fragment() {
    vec2 pixel_size = is_canvas_group ? SCREEN_PIXEL_SIZE : TEXTURE_PIXEL_SIZE;
    vec2 uv = is_canvas_group ? SCREEN_UV : UV;

	float samples = float(pow(2.0, float(sample_power)));
	float layer_increment = strength / samples;
	float angle_increment = TAU / samples;

	// Fetch the color of the processed pixel.
	vec4 color;
	if (is_canvas_group) {
		color = textureLod(screen_texture, uv, 0.0);
	} else {
		color = textureLod(TEXTURE, uv, 0.0);
	}


	// Iterate over samples number of layers. Skip this loop entirely if the strength is 0.
	for (float d = layer_increment; d <= strength && strength > 0.0; d += layer_increment) {
		// Create an empty vec4 to store the average color of the current layer.
		vec4 layer_color = vec4(0.0, 0.0, 0.0, 0.0);

		// Iterate over samples number of angles.
		for (float t = 0.0; t < TAU; t += angle_increment) {
			vec2 sample_uv = uv + rotate(pixel_size * d, t);
			vec4 sample_color;
			if (is_canvas_group) {
				sample_color = textureLod(screen_texture, sample_uv, 0.0);
			} else {
				sample_color = textureLod(TEXTURE, sample_uv, 0.0);
			}
			layer_color += sample_color;
		}

		// Find the average color of the current layer.
		layer_color /= samples;

		// Determine the weight given to the current layer. Farther layers will have less weight.
		float weight = 1.0 - sqrt(d / strength);

		// Mix the current layer's color with the processed pixel's color by the calculated weight.
		color = mix(color, layer_color, weight);
	}

	if (is_canvas_group) {
		color.rgb /= color.a > 0.0001 ? color.a : 1.0;
		COLOR *= color;
	} else {
		COLOR = color;
	}
}
