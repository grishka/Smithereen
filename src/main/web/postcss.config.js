module.exports={
	syntax: 'postcss-scss',
	plugins:[
		require('postcss-advanced-variables')(),
		require('postcss-import')(),
		require('postcss-color-function')(),
		require('postcss-rgba-hex')({rgbOnly: true, silent: true}),
		require('postcss-nested')(),
		require('postcss-inline-svg')(),
		require('autoprefixer')(),
		require('cssnano')({preset: ['default', {}]})
	]
}