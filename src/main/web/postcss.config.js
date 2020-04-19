module.exports={
	plugins:[
		require('postcss-advanced-variables')(),
		require('postcss-color-function')(),
		require('postcss-rgba-hex')({rgbOnly: true, silent: true}),
		require('postcss-calc')(),
		require('postcss-nested')(),
		require('postcss-inline-svg')(),
		require('autoprefixer')()
	]
}