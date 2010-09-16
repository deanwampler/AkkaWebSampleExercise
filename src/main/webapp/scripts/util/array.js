// Helpers for Arrays

// From Crockford's "JavaScript, the Good Parts", pg. 61.
function is_array(thing) {
	return Object.prototype.toString.apply(thing) === '[object Array]'
}